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
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.MediaStreamTrack
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

private const val TAG = "WebRtcBroadcast"

/**
 * Server protocol (broadcaster side):
 *
 * URL: ws://HOST/api/webrtc/ws/broadcast/{stream_code}?token=<jwt>
 *
 * On connect → RX {"type":"connected","role":"broadcaster",...}
 * Viewer joins → RX {"type":"new_viewer","viewer_id":42}           ← store viewer_id (Int)
 * Viewer offer → RX {"type":"offer","sdp":"v=0...","source":"viewer_42_123456"}
 *                    → extract viewer_id from source field
 *                    → setRemoteDescription(offer) → createAnswer
 * TX answer   → {"type":"answer","sdp":"...","target":42}          ← target = Int viewer_id
 * TX ICE      → {"type":"ice_candidate","candidate":"...","sdpMid":"0","sdpMLineIndex":0,"target":42}
 * RX ICE      → {"type":"ice_candidate","candidate":"...","sdpMid":"0","sdpMLineIndex":0,"source":"viewer_42_..."}
 */
class WebRtcBroadcastManager(
    private val context: Context,
    private val streamCode: String,
    private val streamKey: String,
    private val jwtToken: String
) {
    companion object {
        private const val WS_HOST = "wss://vikplay-backend.onrender.com"
        // Server expects ONLY token — no key param
        fun broadcastUrl(code: String, token: String) =
            "$WS_HOST/api/webrtc/ws/broadcast/$code?token=$token"
    }

    val eglBase: EglBase = EglBase.create()

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private val peerConnections = ConcurrentHashMap<Int, PeerConnection>()

    // ICE candidates that arrive before setRemoteDescription completes
    private val pendingCandidates = ConcurrentHashMap<Int, CopyOnWriteArrayList<IceCandidate>>()
    // Track which PeerConnections have completed setRemoteDescription
    private val remoteDescSet = ConcurrentHashMap<Int, Boolean>()

    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var webSocket: WebSocket? = null

    var videoTrack: VideoTrack? = null;  private set
    var audioTrack: AudioTrack? = null;  private set

    // Fetched from /api/webrtc/ice-servers; populated before first PeerConnection is created
    private var cachedIceServers: List<PeerConnection.IceServer> = emptyList()

    // ── Public API ──

    fun start() {
        initPeerConnectionFactory()
        fetchIceServersAndConnect()
    }

    /** Fetches fresh ICE/TURN credentials from the backend (on a background thread), then opens the WebSocket. */
    private fun fetchIceServersAndConnect() {
        Thread {
            val httpClient = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            val req = Request.Builder()
                .url("https://vikplay-backend.onrender.com/api/webrtc/ice-servers")
                .header("Authorization", "Bearer $jwtToken")
                .get()
                .build()
            try {
                val resp = httpClient.newCall(req).execute()
                val body = resp.body?.string() ?: ""
                Log.d(TAG, "ICE servers response [${resp.code}]: $body")
                if (resp.isSuccessful && body.isNotBlank()) {
                    val parsed = parseIceServers(
                        JSONObject(body).optJSONArray("ice_servers") ?: JSONArray()
                    )
                    if (parsed.isNotEmpty()) {
                        cachedIceServers = parsed
                        Log.d(TAG, "Loaded ${cachedIceServers.size} ICE servers from backend")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "fetchIceServers failed (${e.javaClass.simpleName}: ${e.message}) — will use fallback")
            }

            // Ensure we always have at least STUN
            val hasTurn = cachedIceServers.any { srv ->
                srv.urls.any { it.startsWith("turn:") || it.startsWith("turns:") }
            }
            if (!hasTurn) {
                Log.w(TAG, "No TURN from backend — adding OpenRelay fallback TURN servers")
                // openrelay.metered.ca is the public demo endpoint; credentials are the fixed public ones.
                // DO NOT use global.relay.metered.ca — that requires private API credentials.
                cachedIceServers = cachedIceServers + listOf(
                    PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
                    PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
                    PeerConnection.IceServer.builder("stun:openrelay.metered.ca:80").createIceServer(),
                    PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
                        .setUsername("openrelayproject").setPassword("openrelayproject").createIceServer(),
                    PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80?transport=tcp")
                        .setUsername("openrelayproject").setPassword("openrelayproject").createIceServer(),
                    PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443")
                        .setUsername("openrelayproject").setPassword("openrelayproject").createIceServer(),
                    PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp")
                        .setUsername("openrelayproject").setPassword("openrelayproject").createIceServer()
                )
            }
            Log.d(TAG, "Final ICE config: ${cachedIceServers.size} servers, hasTurn=${ 
                cachedIceServers.any { s -> s.urls.any { it.startsWith("turn:") || it.startsWith("turns:") } }
            }")
            openWebSocket()
        }.start()
    }

    /**
     * Parses the ice_servers JSON array returned by /api/webrtc/ice-servers.
     * Each entry: {"urls":"stun:..."}  or  {"urls":"turn:...","username":"...","credential":"..."}
     * The "urls" field can be a String or a JSON array of strings.
     */
    private fun parseIceServers(arr: JSONArray): List<PeerConnection.IceServer> {
        val servers = mutableListOf<PeerConnection.IceServer>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val username   = obj.optString("username", "")
            val credential = obj.optString("credential", "")
            // urls can be String or JSONArray
            val urlsRaw = obj.opt("urls")
            val urlList = when (urlsRaw) {
                is String    -> listOf(urlsRaw)
                is JSONArray -> (0 until urlsRaw.length()).map { urlsRaw.getString(it) }
                else         -> continue
            }
            for (url in urlList) {
                val builder = PeerConnection.IceServer.builder(url)
                if (username.isNotBlank())   builder.setUsername(username)
                if (credential.isNotBlank()) builder.setPassword(credential)
                servers.add(builder.createIceServer())
                Log.d(TAG, "  ICE: $url${if (username.isNotBlank()) " (TURN)" else ""} ")
            }
        }
        return servers
    }

    fun release() {
        try { webSocket?.close(1000, "Stream stopped") } catch (_: Exception) {}
        try { videoCapturer?.stopCapture() } catch (_: Exception) {}
        try {
            peerConnections.values.forEach { runCatching { it.close() }; runCatching { it.dispose() } }
            peerConnections.clear()
            pendingCandidates.clear()
            remoteDescSet.clear()
            videoCapturer?.dispose()
            videoTrack?.dispose()
            audioTrack?.dispose()
            videoSource?.dispose()
            audioSource?.dispose()
            surfaceTextureHelper?.dispose()
            peerConnectionFactory?.dispose()
            eglBase.release()
        } catch (e: Exception) { Log.e(TAG, "release: ${e.message}") }
    }

    // ── Init ──

    private fun initPeerConnectionFactory() {
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).createInitializationOptions()
        )
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .createPeerConnectionFactory()
        setupMediaTracks()
    }

    private fun setupMediaTracks() {
        val factory = peerConnectionFactory ?: return
        surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThr", eglBase.eglBaseContext)
        videoCapturer = createFrontCameraCapturer()
        videoSource = factory.createVideoSource(videoCapturer?.isScreencast ?: false)
        videoCapturer?.initialize(surfaceTextureHelper, context, videoSource!!.capturerObserver)
        videoCapturer?.startCapture(1280, 720, 30)
        videoTrack = factory.createVideoTrack("ARDAMSv0", videoSource)
        audioSource = factory.createAudioSource(MediaConstraints())
        audioTrack  = factory.createAudioTrack("ARDAMSa0", audioSource)
        Log.d(TAG, "Media tracks: video=${videoTrack != null} audio=${audioTrack != null}")
    }

    private fun createFrontCameraCapturer(): VideoCapturer? {
        val en = Camera2Enumerator(context)
        en.deviceNames.firstOrNull { en.isFrontFacing(it) }?.let { return en.createCapturer(it, null) }
        en.deviceNames.firstOrNull { en.isBackFacing(it) }?.let  { return en.createCapturer(it, null) }
        return null
    }

    // ── WebSocket ──

    private fun openWebSocket() {
        val url = broadcastUrl(streamCode, jwtToken)
        Log.d(TAG, "WS opening: $url")

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .pingInterval(15, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder().url(url).build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "WS onOpen HTTP ${response.code}")
                // No register message needed — server sends "connected" automatically
            }
            override fun onMessage(ws: WebSocket, text: String) {
                Log.d(TAG, "RX: $text")
                handleSignalingMessage(text)
            }
            override fun onMessage(ws: WebSocket, bytes: ByteString) {
                val text = bytes.utf8()
                Log.d(TAG, "RX bin: $text")
                handleSignalingMessage(text)
            }
            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                Log.w(TAG, "WS onClosing [$code] $reason")
                ws.close(1000, null)
            }
            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WS onFailure: ${t.message}", t)
                Log.e(TAG, "  resp: ${response?.code} ${response?.message}")
            }
            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WS onClosed [$code] $reason")
            }
        })
    }

    // ── Signaling ──

    /**
     * Extract viewer ID (as Int) from server message.
     *
     * new_viewer → "viewer_id":618849
     * offer/ice  → "sender_id":618849  OR  "source":"viewer_618849_629625"
     */
    private fun extractViewerId(json: JSONObject): Int? {
        // "viewer_id" field (integer in new_viewer message)
        if (json.has("viewer_id")) return json.optInt("viewer_id", -1).takeIf { it >= 0 }
        // "sender_id" field (integer in offer/ice messages)
        if (json.has("sender_id")) return json.optInt("sender_id", -1).takeIf { it >= 0 }
        // "source" field fallback: "viewer_618849_629625" → extract 618849
        json.optString("source").takeIf { it.startsWith("viewer_") }?.let { src ->
            val parts = src.split("_")
            if (parts.size >= 2) return parts[1].toIntOrNull()
        }
        return null
    }

    private fun handleSignalingMessage(text: String) {
        try {
            val json = JSONObject(text)
            val type = json.optString("type")
            val viewerId = extractViewerId(json)

            when (type) {
                "offer" -> {
                    if (viewerId == null) { Log.e(TAG, "offer: no viewer id found"); return }
                    // Server sends nested: {"type":"offer","offer":{"type":"offer","sdp":"..."},...}
                    // Fallback: flat {"type":"offer","sdp":"..."}
                    val sdp = when {
                        json.has("offer") -> json.getJSONObject("offer").optString("sdp", "")
                        json.has("sdp")   -> json.optString("sdp", "")
                        else -> ""
                    }
                    if (sdp.isBlank()) { Log.e(TAG, "offer: no sdp found"); return }
                    Log.d(TAG, "Got offer from viewer $viewerId (${sdp.length} chars)")
                    handleViewerOffer(viewerId, sdp)
                }

                "ice_candidate" -> {
                    if (viewerId == null) { Log.e(TAG, "ice: no viewer id"); return }
                    // Server sends nested: {"candidate":{"candidate":"...","sdpMid":"0","sdpMLineIndex":0},...}
                    // Fallback: flat {"candidate":"...","sdpMid":"0","sdpMLineIndex":0}
                    val c = json.optJSONObject("candidate")
                    val candidateStr: String
                    val sdpMid: String
                    val sdpMLineIndex: Int
                    if (c != null) {
                        candidateStr   = c.optString("candidate", "")
                        sdpMid         = c.optString("sdpMid", "0")
                        sdpMLineIndex  = c.optInt("sdpMLineIndex", 0)
                    } else {
                        candidateStr   = json.optString("candidate", "")
                        sdpMid         = json.optString("sdpMid", "0")
                        sdpMLineIndex  = json.optInt("sdpMLineIndex", 0)
                    }
                    // Empty candidate string = end-of-candidates signal — safe to ignore
                    if (candidateStr.isBlank()) { Log.d(TAG, "ice: end-of-candidates from viewer $viewerId"); return }
                    val candidate = IceCandidate(sdpMid, sdpMLineIndex, candidateStr)
                    addOrQueueCandidate(viewerId, candidate)
                }

                "connected" -> Log.d(TAG, "Server ACK: ${json.optString("message")}")

                // Viewer's answer to our ICE-restart offer
                "answer" -> {
                    if (viewerId == null) { Log.e(TAG, "answer: no viewer id"); return }
                    val sdp = when {
                        json.has("answer") -> json.getJSONObject("answer").optString("sdp", "")
                        json.has("sdp")    -> json.optString("sdp", "")
                        else -> ""
                    }
                    if (sdp.isBlank()) { Log.e(TAG, "answer: no sdp"); return }
                    val pc = peerConnections[viewerId] ?: run {
                        Log.e(TAG, "answer: no PC for viewer $viewerId"); return
                    }
                    Log.d(TAG, "RX answer from viewer $viewerId — setting remote desc (ICE restart)")
                    pc.setRemoteDescription(sdpObserver(
                        onSetSuccess = { Log.d(TAG, "ICE restart setRemote OK [$viewerId]") },
                        onSetFailure = { Log.e(TAG, "ICE restart setRemote FAIL [$viewerId]: $it") }
                    ), SessionDescription(SessionDescription.Type.ANSWER, sdp))
                }

                "new_viewer" -> {
                    Log.d(TAG, "New viewer joined: $viewerId")
                }

                "viewer_count" -> Log.d(TAG, "Viewer count: ${json.optInt("count")}")

                "viewer_left" -> {
                    if (viewerId != null) {
                        peerConnections.remove(viewerId)?.also {
                            runCatching { it.close() }; runCatching { it.dispose() }
                        }
                        pendingCandidates.remove(viewerId)
                        remoteDescSet.remove(viewerId)
                        Log.d(TAG, "Viewer $viewerId left — cleaned up")
                    }
                }

                "error" -> Log.e(TAG, "Server error: ${json.optString("message")}")
                else    -> Log.d(TAG, "Unhandled: $type")
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleSignaling EXCEPTION: ${e.message}", e)
        }
    }

    /**
     * Add ICE candidate to PeerConnection, or queue if setRemoteDescription hasn't completed yet.
     */
    private fun addOrQueueCandidate(viewerId: Int, candidate: IceCandidate) {
        val pc = peerConnections[viewerId]
        if (pc != null && remoteDescSet[viewerId] == true) {
            pc.addIceCandidate(candidate)
            Log.d(TAG, "Added ICE from viewer $viewerId")
        } else {
            pendingCandidates.getOrPut(viewerId) { CopyOnWriteArrayList() }.add(candidate)
            Log.d(TAG, "Queued ICE from viewer $viewerId (pc=${pc != null}, remoteSet=${remoteDescSet[viewerId]})")
        }
    }

    /**
     * Drain queued ICE candidates after setRemoteDescription succeeds.
     */
    private fun drainPendingCandidates(viewerId: Int, pc: PeerConnection) {
        val queued = pendingCandidates.remove(viewerId) ?: return
        Log.d(TAG, "Draining ${queued.size} queued ICE candidates for viewer $viewerId")
        for (c in queued) {
            pc.addIceCandidate(c)
        }
    }

    // ── PeerConnection per viewer ──

    private fun handleViewerOffer(viewerId: Int, offerSdp: String) {
        val factory = peerConnectionFactory ?: run {
            Log.e(TAG, "handleOffer: factory null!"); return
        }
        Log.d(TAG, "handleOffer [viewer $viewerId]: video=${videoTrack != null} audio=${audioTrack != null}")

        // Clean up stale
        peerConnections.remove(viewerId)?.also { runCatching { it.close() }; runCatching { it.dispose() } }
        remoteDescSet.remove(viewerId)

        val config = PeerConnection.RTCConfiguration(cachedIceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            iceTransportsType = PeerConnection.IceTransportsType.ALL
        }

        val pc = factory.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate ?: return
                // Nested format expected by viewer browser:
                // {"type":"ice_candidate","candidate":{"candidate":"...","sdpMid":"0","sdpMLineIndex":0},"target":42}
                val msg = JSONObject().apply {
                    put("type", "ice_candidate")
                    put("candidate", JSONObject().apply {
                        put("candidate", candidate.sdp)
                        put("sdpMid", candidate.sdpMid)
                        put("sdpMLineIndex", candidate.sdpMLineIndex)
                    })
                    put("target", viewerId)
                }
                val sent = webSocket?.send(msg.toString())
                // Extract candidate type (host/srflx/relay) for diagnostics
                val candType = candidate.sdp.substringAfter("typ ").substringBefore(" ").trim()
                Log.d(TAG, "TX ICE → viewer $viewerId type=$candType mid=${candidate.sdpMid} (sent=$sent)")
            }

            override fun onSignalingChange(s: PeerConnection.SignalingState?) {
                Log.d(TAG, "Signal [$viewerId] $s")
            }
            override fun onIceConnectionChange(s: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE conn [$viewerId] $s")
                when (s) {
                    PeerConnection.IceConnectionState.FAILED -> {
                        // ICE restart via broadcaster offer doesn't work if the viewer
                        // ignores incoming offers. Instead, close this PC so the viewer's
                        // own failure detection will cause it to send a fresh offer.
                        Log.w(TAG, "ICE FAILED for viewer $viewerId — closing PC, waiting for viewer retry")
                        peerConnections.remove(viewerId)?.also {
                            runCatching { it.close() }
                            runCatching { it.dispose() }
                        }
                        pendingCandidates.remove(viewerId)
                        remoteDescSet.remove(viewerId)
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED -> {
                        Log.w(TAG, "ICE DISCONNECTED for viewer $viewerId — monitoring...")
                    }
                    PeerConnection.IceConnectionState.CONNECTED,
                    PeerConnection.IceConnectionState.COMPLETED -> {
                        Log.i(TAG, "ICE CONNECTED for viewer $viewerId ✓")
                    }
                    else -> {}
                }
            }
            override fun onIceGatheringChange(s: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "ICE gather [$viewerId] $s")
            }
            override fun onIceConnectionReceivingChange(b: Boolean) {}
            override fun onIceCandidatesRemoved(c: Array<out IceCandidate>?) {}
            override fun onAddStream(s: MediaStream?) {}
            override fun onRemoveStream(s: MediaStream?) {}
            override fun onDataChannel(dc: org.webrtc.DataChannel?) {}
            override fun onRenegotiationNeeded() {
                // Renegotiation is not initiated by the broadcaster — viewer always sends the offer.
                Log.d(TAG, "Renego needed [$viewerId] — ignored (viewer-side offer model)")
            }
            override fun onAddTrack(r: org.webrtc.RtpReceiver?, s: Array<out MediaStream>?) {}
        })

        if (pc == null) { Log.e(TAG, "handleOffer: createPC null!"); return }
        peerConnections[viewerId] = pc
        Log.d(TAG, "PC created for $viewerId")

        // Step 1: Set remote description (the viewer's offer)
        val offer = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        pc.setRemoteDescription(sdpObserver(
            onSetSuccess = {
                Log.d(TAG, "setRemoteDesc OK [$viewerId]")
                remoteDescSet[viewerId] = true

                // Step 2: Drain any ICE candidates that arrived early
                drainPendingCandidates(viewerId, pc)

                // Step 3: Attach our local tracks to the existing transceivers
                try {
                    val transceivers = pc.transceivers
                    Log.d(TAG, "Transceivers: ${transceivers.size}")
                    var videoAttached = false
                    var audioAttached = false
                    for (tr in transceivers) {
                        val media = tr.mediaType
                        val mid   = tr.mid
                        Log.d(TAG, "  tr mid=$mid type=$media dir=${tr.direction}")
                        if (media == MediaStreamTrack.MediaType.MEDIA_TYPE_VIDEO && !videoAttached) {
                            videoTrack?.let {
                                tr.sender.setTrack(it, false)
                                tr.direction = RtpTransceiver.RtpTransceiverDirection.SEND_ONLY
                                videoAttached = true
                                Log.d(TAG, "  → video attached")
                            }
                        } else if (media == MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO && !audioAttached) {
                            audioTrack?.let {
                                tr.sender.setTrack(it, false)
                                tr.direction = RtpTransceiver.RtpTransceiverDirection.SEND_ONLY
                                audioAttached = true
                                Log.d(TAG, "  → audio attached")
                            }
                        }
                    }
                    Log.d(TAG, "Tracks attached: video=$videoAttached audio=$audioAttached")
                } catch (e: Exception) {
                    Log.e(TAG, "Track attach error [$viewerId]: ${e.message}", e)
                }

                // Step 4: Create answer
                pc.createAnswer(sdpObserver(
                    onCreateSuccess = { answerSdp ->
                        Log.d(TAG, "createAnswer OK [$viewerId] len=${answerSdp?.description?.length}")

                        // Step 5: Set local description
                        pc.setLocalDescription(sdpObserver(
                            onSetSuccess = {
                                Log.d(TAG, "setLocalDesc OK [viewer $viewerId] — sending answer")
                                // Nested format expected by viewer browser:
                                // {"type":"answer","answer":{"type":"answer","sdp":"..."},"target":42}
                                val msg = JSONObject().apply {
                                    put("type", "answer")
                                    put("answer", JSONObject().apply {
                                        put("type", "answer")
                                        put("sdp", answerSdp!!.description)
                                    })
                                    put("target", viewerId)
                                }
                                val sent = webSocket?.send(msg.toString())
                                Log.d(TAG, "TX answer → viewer $viewerId (sent=$sent)")
                            },
                            onSetFailure = { Log.e(TAG, "setLocalDesc FAIL [$viewerId]: $it") }
                        ), answerSdp)
                    },
                    onCreateFailure = { Log.e(TAG, "createAnswer FAIL [$viewerId]: $it") }
                ), MediaConstraints())
            },
            onSetFailure = { Log.e(TAG, "setRemoteDesc FAIL [$viewerId]: $it") }
        ), offer)
    }

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