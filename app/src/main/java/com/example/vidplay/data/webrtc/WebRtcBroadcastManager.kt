package com.example.vidplay.data.webrtc

import android.content.Context
import android.os.Handler
import android.os.Looper
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

class WebRtcBroadcastManager(
    private val context: Context,
    private val streamCode: String,
    private val streamKey: String,
    private val jwtToken: String
) {
    companion object {
        private const val WS_HOST = "wss://vikplay-backend.onrender.com"
        
        fun broadcastUrl(code: String, token: String) =
            "$WS_HOST/api/webrtc/ws/broadcast/$code?token=$token"
    }

    val eglBase: EglBase = EglBase.create()

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private val peerConnections = ConcurrentHashMap<Int, PeerConnection>()

    
    private val pendingCandidates = ConcurrentHashMap<Int, CopyOnWriteArrayList<IceCandidate>>()
    
    private val remoteDescSet = ConcurrentHashMap<Int, Boolean>()

    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var webSocket: WebSocket? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var onChatMessageListener: ((String, String, String, Long) -> Unit)? = null

    var videoTrack: VideoTrack? = null;  private set
    var audioTrack: AudioTrack? = null;  private set

    
    private var cachedIceServers: List<PeerConnection.IceServer> = emptyList()

    

    fun start() {
        initPeerConnectionFactory()
        fetchIceServersAndConnect()
    }

    fun setOnChatMessageListener(listener: ((String, String, String, Long) -> Unit)?) {
        onChatMessageListener = listener
    }

    fun sendChatMessage(text: String, username: String) {
        if (text.isBlank()) return
        val msg = JSONObject().apply {
            put("type", "chat_message")
            put("message", text.trim())
            put("username", username.ifBlank { "Broadcaster" })
            put("role", "broadcaster")
            put("timestamp", System.currentTimeMillis())
        }
        webSocket?.send(msg.toString())
    }

    
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

            
            val hasTurn = cachedIceServers.any { srv ->
                srv.urls.any { it.startsWith("turn:") || it.startsWith("turns:") }
            }
            if (!hasTurn) {
                Log.w(TAG, "No TURN from backend — adding OpenRelay fallback TURN servers")
                
                
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

    

    private fun parseIceServers(arr: JSONArray): List<PeerConnection.IceServer> {
        val servers = mutableListOf<PeerConnection.IceServer>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val username   = obj.optString("username", "")
            val credential = obj.optString("credential", "")
            
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
            onChatMessageListener = null
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

    

    

    private fun extractViewerId(json: JSONObject): Int? {
        
        if (json.has("viewer_id")) return json.optInt("viewer_id", -1).takeIf { it >= 0 }
        
        if (json.has("sender_id")) return json.optInt("sender_id", -1).takeIf { it >= 0 }
        
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
                    
                    if (candidateStr.isBlank()) { Log.d(TAG, "ice: end-of-candidates from viewer $viewerId"); return }
                    val candidate = IceCandidate(sdpMid, sdpMLineIndex, candidateStr)
                    addOrQueueCandidate(viewerId, candidate)
                }

                "connected" -> Log.d(TAG, "Server ACK: ${json.optString("message")}")

                
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

                "chat_message" -> {
                    val username = json.optString("username", "Anonymous")
                    val message = json.optString("message", "")
                    val role = json.optString("role", "viewer")
                    val timestamp = json.optLong("timestamp", System.currentTimeMillis())

                    if (message.isNotBlank()) {
                        mainHandler.post {
                            onChatMessageListener?.invoke(username, message, role, timestamp)
                        }
                    }
                }

                "error" -> Log.e(TAG, "Server error: ${json.optString("message")}")
                else    -> Log.d(TAG, "Unhandled: $type")
            }
        } catch (e: Exception) {
            Log.e(TAG, "handleSignaling EXCEPTION: ${e.message}", e)
        }
    }

    

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

    

    private fun drainPendingCandidates(viewerId: Int, pc: PeerConnection) {
        val queued = pendingCandidates.remove(viewerId) ?: return
        Log.d(TAG, "Draining ${queued.size} queued ICE candidates for viewer $viewerId")
        for (c in queued) {
            pc.addIceCandidate(c)
        }
    }

    

    private fun handleViewerOffer(viewerId: Int, offerSdp: String) {
        val factory = peerConnectionFactory ?: run {
            Log.e(TAG, "handleOffer: factory null!"); return
        }
        Log.d(TAG, "handleOffer [viewer $viewerId]: video=${videoTrack != null} audio=${audioTrack != null}")

        
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
                
                Log.d(TAG, "Renego needed [$viewerId] — ignored (viewer-side offer model)")
            }
            override fun onAddTrack(r: org.webrtc.RtpReceiver?, s: Array<out MediaStream>?) {}
        })

        if (pc == null) { Log.e(TAG, "handleOffer: createPC null!"); return }
        peerConnections[viewerId] = pc
        Log.d(TAG, "PC created for $viewerId")

        
        val offer = SessionDescription(SessionDescription.Type.OFFER, offerSdp)
        pc.setRemoteDescription(sdpObserver(
            onSetSuccess = {
                Log.d(TAG, "setRemoteDesc OK [$viewerId]")
                remoteDescSet[viewerId] = true

                
                drainPendingCandidates(viewerId, pc)

                
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

                
                pc.createAnswer(sdpObserver(
                    onCreateSuccess = { answerSdp ->
                        Log.d(TAG, "createAnswer OK [$viewerId] len=${answerSdp?.description?.length}")

                        
                        pc.setLocalDescription(sdpObserver(
                            onSetSuccess = {
                                Log.d(TAG, "setLocalDesc OK [viewer $viewerId] — sending answer")
                                
                                
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