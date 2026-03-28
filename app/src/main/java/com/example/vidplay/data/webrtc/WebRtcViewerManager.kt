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
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

private const val TAG = "WebRtcViewer"

/**
 * Viewer-side WebRTC engine.
 *
 * Confirmed server protocol (from backend source):
 *
 *   WS URL:  wss://HOST/api/webrtc/ws/view/{stream_code}?token=<jwt>
 *
 *   RX "connected"     -> viewer immediately creates PC + sends SDP offer
 *   TX "offer"         -> server auto-relays to broadcaster (sets source=peer_id)
 *   RX "answer"        -> broadcaster's SDP answer routed back via target=viewer_id
 *   TX/RX "ice_candidate" -> relayed both ways by server
 *   RX "stream_ended"  -> broadcaster disconnected
 *   TX "request_go_live" -> triggers keyframe from broadcaster
 *
 *   NOT supported by server: "request_offer" — dropped with warning log.
 */
class WebRtcViewerManager(
    private val context: Context,
    private val streamCode: String,
    private val jwtToken: String,
    private val onRemoteTrack: (VideoTrack) -> Unit,
    private val onStreamEnded: () -> Unit,
    private val onConnected: () -> Unit,
    private val onError: (String) -> Unit
) {
    companion object {
        private const val REST_HOST = "https://vikplay-backend.onrender.com"
        private const val WS_HOST   = "wss://vikplay-backend.onrender.com"

        fun viewerUrl(code: String, token: String) =
            "$WS_HOST/api/webrtc/ws/view/$code?token=$token"
    }

    val eglBase: EglBase = EglBase.create()

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var webSocket: WebSocket? = null
    private var viewerId: Int = -1

    private val pendingCandidates = CopyOnWriteArrayList<IceCandidate>()
    private var remoteDescSet = false

    private var cachedIceServers: List<PeerConnection.IceServer> = emptyList()

    // ── Public API ───────────────────────────────────────────────────────────

    fun start() {
        initFactory()
        fetchIceServersAndConnect()
    }

    fun sendGoLive() {
        webSocket?.send(JSONObject().apply { put("type", "request_go_live") }.toString())
        Log.d(TAG, "TX request_go_live")
    }

    fun sendChatMessage(text: String) {
        val msg = JSONObject().apply { 
            put("type", "chat_message")
            put("message", text)
        }.toString()
        webSocket?.send(msg)
        Log.d(TAG, "TX chat_message: $text")
    }

    fun release() {
        try { webSocket?.close(1000, "Viewer left") } catch (_: Exception) {}
        try { peerConnection?.close(); peerConnection?.dispose() } catch (_: Exception) {}
        try { peerConnectionFactory?.dispose() } catch (_: Exception) {}
        try { eglBase.release() } catch (_: Exception) {}
        pendingCandidates.clear()
        remoteDescSet = false
        viewerId = -1
        Log.d(TAG, "Released")
    }

    // ── Factory ──────────────────────────────────────────────────────────────

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

    // ── ICE fetch ────────────────────────────────────────────────────────────

    private fun fetchIceServersAndConnect() {
        Thread {
            val http = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            try {
                val resp = http.newCall(
                    Request.Builder()
                        .url("$REST_HOST/api/webrtc/ice-servers")
                        .header("Authorization", "Bearer $jwtToken")
                        .get().build()
                ).execute()
                val body = resp.body?.string() ?: ""
                Log.d(TAG, "ICE servers [${resp.code}]: $body")
                if (resp.isSuccessful && body.isNotBlank()) {
                    val arr = JSONObject(body).optJSONArray("ice_servers") ?: JSONArray()
                    parseIceServers(arr).takeIf { it.isNotEmpty() }?.let {
                        cachedIceServers = it
                        Log.d(TAG, "Loaded ${it.size} ICE servers from backend")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "fetchIceServers failed: ${e.message}")
            }

            val hasTurn = cachedIceServers.any { s ->
                s.urls.any { it.startsWith("turn:") || it.startsWith("turns:") }
            }
            if (!hasTurn) {
                Log.w(TAG, "No TURN from backend — adding OpenRelay fallback")
                cachedIceServers = cachedIceServers + listOf(
                    PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                    PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
                        .setUsername("openrelayproject").setPassword("openrelayproject").createIceServer(),
                    PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80?transport=tcp")
                        .setUsername("openrelayproject").setPassword("openrelayproject").createIceServer(),
                    PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443")
                        .setUsername("openrelayproject").setPassword("openrelayproject").createIceServer()
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
            val urls: List<String> = when (urlsRaw) {
                is String    -> listOf(urlsRaw)
                is JSONArray -> (0 until urlsRaw.length()).map { urlsRaw.getString(it) }
                else         -> continue
            }
            for (url in urls) {
                val b = PeerConnection.IceServer.builder(url)
                if (username.isNotBlank())   b.setUsername(username)
                if (credential.isNotBlank()) b.setPassword(credential)
                result.add(b.createIceServer())
            }
        }
        return result
    }

    // ── WebSocket ─────────────────────────────────────────────────────────────

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
                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WS onClosed [$code] $reason")
                }
                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    val body = try { response?.peekBody(512)?.string() } catch (_: Exception) { null }
                    Log.e(TAG, "WS onFailure: ${t.message} resp=${response?.code} body=$body")
                    onError("Connection failed (${response?.code ?: "no response"}): ${t.message}")
                }
            }
        )
    }

    // ── Message handler ───────────────────────────────────────────────────────

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)
            when (val type = json.optString("type")) {

                "connected" -> {
                    // Server confirmed viewer WS + assigned viewer_id.
                    // Protocol: viewer immediately sends SDP offer.
                    // Server auto-relays it to broadcaster (sets source=peer_id).
                    val vid = json.optInt("viewer_id", -1)
                    viewerId = vid
                    Log.d(TAG, "connected: viewer_id=$vid stream=$streamCode -> creating PC + sending offer")
                    createPcAndSendOffer(vid)
                }

                "answer" -> {
                    // Broadcaster responded to our offer — server routed it here via target=viewer_id
                    val sdp = when {
                        json.has("answer") -> json.getJSONObject("answer").optString("sdp", "")
                        json.has("sdp")    -> json.optString("sdp", "")
                        else               -> ""
                    }
                    if (sdp.isBlank()) { Log.e(TAG, "answer: no sdp in: $text"); return }
                    Log.d(TAG, "answer received (${sdp.length} chars) — setRemoteDescription")
                    handleAnswer(sdp)
                }

                "ice_candidate" -> {
                    // ICE candidate relayed from broadcaster
                    val c             = json.optJSONObject("candidate")
                    val candidateStr  = c?.optString("candidate", "")  ?: json.optString("candidate", "")
                    val sdpMid        = c?.optString("sdpMid", "0")     ?: json.optString("sdpMid", "0")
                    val sdpMLineIndex = c?.optInt("sdpMLineIndex", 0)   ?: json.optInt("sdpMLineIndex", 0)
                    if (candidateStr.isBlank()) { Log.d(TAG, "end-of-candidates"); return }
                    Log.d(TAG, "ice_candidate from broadcaster mid=$sdpMid")
                    addOrQueueCandidate(IceCandidate(sdpMid, sdpMLineIndex, candidateStr))
                }

                "stream_ended" -> {
                    Log.i(TAG, "stream_ended — broadcaster disconnected")
                    onStreamEnded()
                }

                "sync_timestamp" -> { /* server heartbeat — ignore */ }

                "chat_message" -> {
                    val username = json.optString("username", "Anonymous")
                    val message  = json.optString("message", "")
                    val role     = json.optString("role", "viewer")
                    Log.d(TAG, "RX chat_message from $username ($role): $message")
                    // onChatMessage will be called if callback is set
                }

                "error" -> {
                    val msg = json.optString("message", "Unknown server error")
                    Log.e(TAG, "server error: $msg")
                    onError(msg)
                }

                else -> Log.d(TAG, "Unhandled message type: $type | $text")
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleMessage exception: ${e.message}", e)
        }
    }

    // ── PeerConnection + SDP offer ────────────────────────────────────────────

    private fun createPcAndSendOffer(vid: Int) {
        val factory = peerConnectionFactory ?: run {
            Log.e(TAG, "createPcAndSendOffer: factory is null"); return
        }

        val config = PeerConnection.RTCConfiguration(cachedIceServers).apply {
            sdpSemantics             = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceTransportsType        = PeerConnection.IceTransportsType.ALL
        }

        val pc = factory.createPeerConnection(config, object : PeerConnection.Observer {

            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate ?: return
                // Server auto-relays to broadcaster because this is a viewer WS connection
                val msg = JSONObject().apply {
                    put("type", "ice_candidate")
                    put("candidate", JSONObject().apply {
                        put("candidate",    candidate.sdp)
                        put("sdpMid",       candidate.sdpMid)
                        put("sdpMLineIndex", candidate.sdpMLineIndex)
                    })
                }.toString()
                webSocket?.send(msg)
                val t = candidate.sdp.substringAfter("typ ").substringBefore(" ").trim()
                Log.d(TAG, "TX ice_candidate type=$t mid=${candidate.sdpMid}")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE state: $state")
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED ->
                        Log.i(TAG, "P2P connected successfully")
                    PeerConnection.IceConnectionState.FAILED ->
                        Log.w(TAG, "ICE FAILED")
                    PeerConnection.IceConnectionState.DISCONNECTED ->
                        Log.w(TAG, "ICE DISCONNECTED")
                    else -> {}
                }
            }

            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                val track = receiver?.track()
                Log.d(TAG, "onAddTrack: kind=${track?.kind()} enabled=${track?.enabled()}")
                if (track is VideoTrack) {
                    Log.i(TAG, "Remote VideoTrack received — handing to UI")
                    onRemoteTrack(track)
                }
            }

            override fun onSignalingChange(s: PeerConnection.SignalingState?)        = Unit
            override fun onIceGatheringChange(s: PeerConnection.IceGatheringState?) = Unit
            override fun onIceConnectionReceivingChange(b: Boolean)                  = Unit
            override fun onIceCandidatesRemoved(c: Array<out IceCandidate>?)         = Unit
            override fun onAddStream(s: MediaStream?)                                = Unit
            override fun onRemoveStream(s: MediaStream?)                             = Unit
            override fun onDataChannel(dc: DataChannel?)                             = Unit
            override fun onRenegotiationNeeded()                                     = Unit
        }) ?: run { Log.e(TAG, "createPeerConnection returned null"); return }

        peerConnection = pc

        pc.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
        )
        pc.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.RECV_ONLY)
        )
        Log.d(TAG, "PC created with 2 RECV_ONLY transceivers — creating offer")

        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        pc.createOffer(sdpObserver(
            onCreateSuccess = { offerSdp ->
                Log.d(TAG, "createOffer OK len=${offerSdp?.description?.length}")
                pc.setLocalDescription(sdpObserver(
                    onSetSuccess = {
                        // Include viewer_id so broadcaster's extractViewerId() can identify us.
                        // Also include source in "viewer_{id}_{ts}" format for web broadcaster JS.
                        // Server auto-relays this to broadcaster; no "target" field needed.
                        val source = "viewer_${vid}_${System.currentTimeMillis()}"
                        val msg = JSONObject().apply {
                            put("type", "offer")
                            put("offer", JSONObject().apply {
                                put("type", "offer")
                                put("sdp", offerSdp!!.description)
                            })
                            put("sdp", offerSdp!!.description)   // flat fallback
                            put("viewer_id", vid)
                            put("sender_id", vid)   // extra fallback field
                            put("source", source)
                        }.toString()
                        val sent = webSocket?.send(msg)
                        Log.d(TAG, "TX offer sent=$sent viewer_id=$vid source=$source len=${msg.length}")
                        Log.d(TAG, "TX offer preview: ${msg.take(300)}")
                    },
                    onSetFailure = { Log.e(TAG, "setLocalDescription FAIL: $it") }
                ), offerSdp)
            },
            onCreateFailure = { Log.e(TAG, "createOffer FAIL: $it") }
        ), constraints)
    }

    // ── Answer ────────────────────────────────────────────────────────────────

    private fun handleAnswer(answerSdp: String) {
        val pc = peerConnection ?: run { Log.e(TAG, "handleAnswer: pc is null"); return }
        pc.setRemoteDescription(sdpObserver(
            onSetSuccess = {
                Log.d(TAG, "setRemoteDescription(answer) OK — ICE exchange in progress")
                remoteDescSet = true
                drainPendingCandidates(pc)
            },
            onSetFailure = { Log.e(TAG, "setRemoteDescription(answer) FAIL: $it") }
        ), SessionDescription(SessionDescription.Type.ANSWER, answerSdp))
    }

    // ── ICE queuing ───────────────────────────────────────────────────────────

    private fun addOrQueueCandidate(candidate: IceCandidate) {
        val pc = peerConnection
        if (pc != null && remoteDescSet) {
            pc.addIceCandidate(candidate)
            Log.d(TAG, "addIceCandidate immediately")
        } else {
            pendingCandidates.add(candidate)
            Log.d(TAG, "queued ICE (remoteSet=$remoteDescSet pcNull=${pc == null})")
        }
    }

    private fun drainPendingCandidates(pc: PeerConnection) {
        if (pendingCandidates.isEmpty()) return
        Log.d(TAG, "draining ${pendingCandidates.size} queued ICE candidates")
        pendingCandidates.forEach { pc.addIceCandidate(it) }
        pendingCandidates.clear()
    }

    // ── SdpObserver helper ────────────────────────────────────────────────────

    private fun sdpObserver(
        onCreateSuccess: ((SessionDescription?) -> Unit)? = null,
        onSetSuccess:    (() -> Unit)? = null,
        onCreateFailure: ((String?) -> Unit)? = null,
        onSetFailure:    ((String?) -> Unit)? = null
    ): SdpObserver = object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) { onCreateSuccess?.invoke(sdp) }
        override fun onSetSuccess()                            { onSetSuccess?.invoke() }
        override fun onCreateFailure(err: String?)             { onCreateFailure?.invoke(err) }
        override fun onSetFailure(err: String?)                { onSetFailure?.invoke(err) }
    }
}