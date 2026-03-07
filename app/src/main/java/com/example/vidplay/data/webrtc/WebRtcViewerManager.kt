package com.example.vidplay.data.webrtc

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

private const val TAG = "WebRtcViewer"

/**
 * Manages the viewer side of a WebRTC live-stream connection.
 *
 * Server protocol (viewer side):
 *   URL:  wss://HOST/api/webrtc/ws/viewer/{stream_code}?token=<jwt>
 *
 *   On WS open  → server notifies broadcaster: {"type":"new_viewer","viewer_id":<id>}
 *   RX offer    → {"type":"offer","offer":{"type":"offer","sdp":"..."}}  (or flat "sdp")
 *                 → setRemoteDescription → createAnswer
 *   TX answer   → {"type":"answer","answer":{"type":"answer","sdp":"..."}}
 *   RX ICE      → {"type":"ice_candidate","candidate":{"candidate":"...","sdpMid":"0","sdpMLineIndex":0}}
 *   TX ICE      → same nested structure
 *   RX ended    → {"type":"stream_ended"}  → onStreamEnded()
 *   TX go-live  → {"type":"request_go_live"}  (sent when buffer drifts behind live)
 */
class WebRtcViewerManager(
    private val context: Context,
    private val streamCode: String,
    private val jwtToken: String,
    /** Called on a background thread when the broadcaster's video/audio track arrives. */
    private val onRemoteTrack: (VideoTrack) -> Unit,
    /** Called on a background thread when the broadcaster disconnects. */
    private val onStreamEnded: () -> Unit,
    /** Called on a background thread when the WebSocket opens (waiting for offer). */
    private val onConnected: () -> Unit,
    /** Called on a background thread when a fatal error occurs. */
    private val onError: (String) -> Unit
) {
    companion object {
        private const val WS_HOST = "wss://vikplay-backend.onrender.com"

        fun viewerUrl(code: String, token: String): String =
            "$WS_HOST/api/webrtc/ws/view/$code?token=$token"
    }

    /** Shared EGL context — must be passed to SurfaceViewRenderer.init(). */
    val eglBase: EglBase = EglBase.create()

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var webSocket: WebSocket? = null

    // ICE candidates queued before setRemoteDescription completes
    private val pendingCandidates = CopyOnWriteArrayList<IceCandidate>()
    private var remoteDescSet = false

    private var cachedIceServers: List<PeerConnection.IceServer> = emptyList()

    // ── Public API ──────────────────────────────────────────────────────────

    /** Initialise WebRTC factory, fetch ICE config, then open the viewer WebSocket. */
    fun start() {
        initFactory()
        fetchIceServersAndConnect()
    }

    /** Send a keyframe / go-live request when the viewer's buffer falls behind. */
    fun sendGoLive() {
        val msg = JSONObject().apply { put("type", "request_go_live") }.toString()
        webSocket?.send(msg)
        Log.d(TAG, "TX request_go_live")
    }

    /** Release all resources. Call from DisposableEffect.onDispose. */
    fun release() {
        try { webSocket?.close(1000, "Viewer left") } catch (_: Exception) {}
        try { peerConnection?.close(); peerConnection?.dispose() } catch (_: Exception) {}
        try { peerConnectionFactory?.dispose() } catch (_: Exception) {}
        try { eglBase.release() } catch (_: Exception) {}
        pendingCandidates.clear()
        remoteDescSet = false
        Log.d(TAG, "Released")
    }

    // ── Initialisation ───────────────────────────────────────────────────────

    private fun initFactory() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
        Log.d(TAG, "PeerConnectionFactory initialised")
    }

    // ── ICE server fetch ─────────────────────────────────────────────────────

    private fun fetchIceServersAndConnect() {
        Thread {
            val http = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            try {
                val resp = http.newCall(
                    Request.Builder()
                        .url("https://vikplay-backend.onrender.com/api/webrtc/ice-servers")
                        .header("Authorization", "Bearer $jwtToken")
                        .get()
                        .build()
                ).execute()
                val body = resp.body?.string() ?: ""
                Log.d(TAG, "ICE servers [${resp.code}]: $body")
                if (resp.isSuccessful && body.isNotBlank()) {
                    val arr = JSONObject(body).optJSONArray("ice_servers") ?: JSONArray()
                    val parsed = parseIceServers(arr)
                    if (parsed.isNotEmpty()) {
                        cachedIceServers = parsed
                        Log.d(TAG, "Loaded ${cachedIceServers.size} ICE servers from backend")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "fetchIceServers failed: ${e.message} — will use fallback")
            }

            // Ensure we always have TURN fallback
            val hasTurn = cachedIceServers.any { s ->
                s.urls.any { it.startsWith("turn:") || it.startsWith("turns:") }
            }
            if (!hasTurn) {
                Log.w(TAG, "No TURN from backend — adding OpenRelay fallback")
                cachedIceServers = cachedIceServers + listOf(
                    PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                    PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
                    PeerConnection.IceServer.builder("stun:openrelay.metered.ca:80").createIceServer(),
                    PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
                        .setUsername("openrelayproject").setPassword("openrelayproject")
                        .createIceServer(),
                    PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80?transport=tcp")
                        .setUsername("openrelayproject").setPassword("openrelayproject")
                        .createIceServer(),
                    PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443")
                        .setUsername("openrelayproject").setPassword("openrelayproject")
                        .createIceServer(),
                    PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp")
                        .setUsername("openrelayproject").setPassword("openrelayproject")
                        .createIceServer()
                )
            }

            Log.d(TAG, "Final ICE config: ${cachedIceServers.size} servers")
            openWebSocket()
        }.start()
    }

    private fun parseIceServers(arr: JSONArray): List<PeerConnection.IceServer> {
        val result = mutableListOf<PeerConnection.IceServer>()
        for (i in 0 until arr.length()) {
            val obj        = arr.optJSONObject(i) ?: continue
            val username   = obj.optString("username", "")
            val credential = obj.optString("credential", "")
            val urlsRaw    = obj.opt("urls")
            val urlList: List<String> = when (urlsRaw) {
                is String    -> listOf(urlsRaw)
                is JSONArray -> (0 until urlsRaw.length()).map { urlsRaw.getString(it) }
                else         -> continue
            }
            for (url in urlList) {
                val builder = PeerConnection.IceServer.builder(url)
                if (username.isNotBlank())   builder.setUsername(username)
                if (credential.isNotBlank()) builder.setPassword(credential)
                result.add(builder.createIceServer())
            }
        }
        return result
    }

    // ── WebSocket ────────────────────────────────────────────────────────────

    private fun openWebSocket() {
        val url = viewerUrl(streamCode, jwtToken)
        Log.d(TAG, "Opening viewer WS: $url")

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .pingInterval(15, TimeUnit.SECONDS)
            .build()

        webSocket = client.newWebSocket(
            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $jwtToken")
                .build(),
            object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.d(TAG, "WS onOpen ${response.code}")
                    // Create the PeerConnection immediately so it's ready when offer arrives
                    createPeerConnection()
                    onConnected()
                }
                override fun onMessage(ws: WebSocket, text: String) {
                    Log.d(TAG, "RX: $text")
                    handleMessage(text)
                }
                override fun onMessage(ws: WebSocket, bytes: ByteString) = handleMessage(bytes.utf8())
                override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                    ws.close(1000, null)
                    Log.w(TAG, "WS onClosing [$code] $reason")
                }
                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    val bodySnippet = try { response?.peekBody(512)?.string() } catch (_: Exception) { null }
                    Log.e(TAG, "WS onFailure: ${t.message} resp=${response?.code} body=$bodySnippet")
                    onError("Connection failed (${response?.code ?: "no response"}): ${t.message}")
                }
                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WS onClosed [$code] $reason")
                }
            }
        )
    }

    // ── PeerConnection ───────────────────────────────────────────────────────

    private fun createPeerConnection() {
        val factory = peerConnectionFactory ?: run {
            Log.e(TAG, "createPeerConnection: factory is null")
            return
        }

        val config = PeerConnection.RTCConfiguration(cachedIceServers).apply {
            sdpSemantics            = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceTransportsType       = PeerConnection.IceTransportsType.ALL
        }

        peerConnection = factory.createPeerConnection(config, object : PeerConnection.Observer {

            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate ?: return
                // Send the candidate to the broadcaster through the signaling server
                val msg = JSONObject().apply {
                    put("type", "ice_candidate")
                    put("candidate", JSONObject().apply {
                        put("candidate",     candidate.sdp)
                        put("sdpMid",         candidate.sdpMid)
                        put("sdpMLineIndex",  candidate.sdpMLineIndex)
                    })
                }
                webSocket?.send(msg.toString())
                val candType = candidate.sdp.substringAfter("typ ").substringBefore(" ").trim()
                Log.d(TAG, "TX ICE type=$candType mid=${candidate.sdpMid}")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE state: $state")
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED ->
                        Log.i(TAG, "P2P connection established ✓")

                    PeerConnection.IceConnectionState.FAILED -> {
                        // Notify the broadcaster to flush a keyframe so the viewer can re-sync
                        Log.w(TAG, "ICE FAILED — sending request_go_live")
                        sendGoLive()
                    }

                    PeerConnection.IceConnectionState.DISCONNECTED ->
                        Log.w(TAG, "ICE DISCONNECTED — monitoring…")

                    else -> {}
                }
            }

            // Called when the broadcaster's track is successfully added
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                val track = receiver?.track()
                if (track is VideoTrack) {
                    Log.i(TAG, "Remote video track received ✓")
                    onRemoteTrack(track)
                } else {
                    Log.d(TAG, "Remote track received: ${track?.kind()}")
                }
            }

            override fun onSignalingChange(s: PeerConnection.SignalingState?)            = Unit
            override fun onIceGatheringChange(s: PeerConnection.IceGatheringState?)     = Unit
            override fun onIceConnectionReceivingChange(b: Boolean)                      = Unit
            override fun onIceCandidatesRemoved(c: Array<out IceCandidate>?)             = Unit
            override fun onAddStream(s: MediaStream?)                                    = Unit
            override fun onRemoveStream(s: MediaStream?)                                 = Unit
            override fun onDataChannel(dc: DataChannel?)                                 = Unit
            // Renegotiation is always initiated by the broadcaster sending a new offer
            override fun onRenegotiationNeeded()                                         = Unit
        })

        Log.d(TAG, "PeerConnection created — waiting for broadcaster offer")
    }

    // ── Signaling message handler ────────────────────────────────────────────

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            when (val type = json.optString("type")) {
                "offer" -> {
                    // Server may send a nested offer object or a flat sdp string
                    val sdp = when {
                        json.has("offer") -> json.getJSONObject("offer").optString("sdp", "")
                        json.has("sdp")   -> json.optString("sdp", "")
                        else              -> ""
                    }
                    if (sdp.isBlank()) { Log.e(TAG, "offer: no sdp found"); return }
                    Log.d(TAG, "Got offer (${sdp.length} chars) — processing")
                    handleOffer(sdp)
                }

                "ice_candidate" -> {
                    // Server may send a nested candidate object or flat fields
                    val c = json.optJSONObject("candidate")
                    val candidateStr: String
                    val sdpMid: String
                    val sdpMLineIndex: Int
                    if (c != null) {
                        candidateStr  = c.optString("candidate", "")
                        sdpMid        = c.optString("sdpMid", "0")
                        sdpMLineIndex = c.optInt("sdpMLineIndex", 0)
                    } else {
                        candidateStr  = json.optString("candidate", "")
                        sdpMid        = json.optString("sdpMid", "0")
                        sdpMLineIndex = json.optInt("sdpMLineIndex", 0)
                    }
                    // Empty string = end-of-candidates signal — safe to ignore
                    if (candidateStr.isBlank()) {
                        Log.d(TAG, "end-of-candidates"); return
                    }
                    addOrQueueCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidateStr))
                }

                "stream_ended" -> {
                    Log.i(TAG, "Stream ended — broadcaster disconnected")
                    onStreamEnded()
                }

                "connected" -> Log.d(TAG, "Server ACK: ${json.optString("message")}")

                "error" -> {
                    val msg = json.optString("message", "Unknown server error")
                    Log.e(TAG, "Server error: $msg")
                    onError(msg)
                }

                else -> Log.d(TAG, "Unhandled message type: $type")
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleMessage exception: ${e.message}", e)
        }
    }

    // ── Offer/Answer negotiation ─────────────────────────────────────────────

    /**
     * Process the broadcaster's SDP offer:
     *   1. setRemoteDescription(offer)
     *   2. drain queued ICE candidates
     *   3. createAnswer
     *   4. setLocalDescription(answer)
     *   5. send answer to server → relayed to broadcaster
     */
    private fun handleOffer(offerSdp: String) {
        val pc = peerConnection ?: run { Log.e(TAG, "handleOffer: pc is null"); return }
        val offer = SessionDescription(SessionDescription.Type.OFFER, offerSdp)

        pc.setRemoteDescription(sdpObserver(
            onSetSuccess = {
                Log.d(TAG, "setRemoteDescription OK")
                remoteDescSet = true
                drainPendingCandidates(pc)

                pc.createAnswer(sdpObserver(
                    onCreateSuccess = { answerSdp ->
                        Log.d(TAG, "createAnswer OK (len=${answerSdp?.description?.length})")
                        pc.setLocalDescription(sdpObserver(
                            onSetSuccess = {
                                Log.d(TAG, "setLocalDescription OK — sending answer")
                                val msg = JSONObject().apply {
                                    put("type", "answer")
                                    put("answer", JSONObject().apply {
                                        put("type", "answer")
                                        put("sdp", answerSdp!!.description)
                                    })
                                }
                                val sent = webSocket?.send(msg.toString())
                                Log.d(TAG, "TX answer (sent=$sent)")
                            },
                            onSetFailure = { Log.e(TAG, "setLocalDescription FAIL: $it") }
                        ), answerSdp)
                    },
                    onCreateFailure = { Log.e(TAG, "createAnswer FAIL: $it") }
                ), MediaConstraints())
            },
            onSetFailure = { Log.e(TAG, "setRemoteDescription FAIL: $it") }
        ), offer)
    }

    // ── ICE candidate queuing ────────────────────────────────────────────────

    private fun addOrQueueCandidate(candidate: IceCandidate) {
        val pc = peerConnection
        if (pc != null && remoteDescSet) {
            pc.addIceCandidate(candidate)
            Log.d(TAG, "Added ICE candidate immediately")
        } else {
            pendingCandidates.add(candidate)
            Log.d(TAG, "Queued ICE candidate (remoteSet=$remoteDescSet, pcNull=${pc == null})")
        }
    }

    private fun drainPendingCandidates(pc: PeerConnection) {
        if (pendingCandidates.isEmpty()) return
        Log.d(TAG, "Draining ${pendingCandidates.size} queued ICE candidates")
        for (c in pendingCandidates) {
            pc.addIceCandidate(c)
        }
        pendingCandidates.clear()
    }

    // ── SdpObserver builder ──────────────────────────────────────────────────

    private fun sdpObserver(
        onCreateSuccess: ((SessionDescription?) -> Unit)? = null,
        onSetSuccess:    (() -> Unit)? = null,
        onCreateFailure: ((String?) -> Unit)? = null,
        onSetFailure:    ((String?) -> Unit)? = null
    ): SdpObserver = object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) { onCreateSuccess?.invoke(sdp) }
        override fun onSetSuccess()                             { onSetSuccess?.invoke() }
        override fun onCreateFailure(err: String?)              { onCreateFailure?.invoke(err) }
        override fun onSetFailure(err: String?)                 { onSetFailure?.invoke(err) }
    }
}
