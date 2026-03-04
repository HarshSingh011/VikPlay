package com.example.vidplay.data.webrtc

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
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
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack
import java.util.concurrent.TimeUnit

private const val TAG = "WebRtcBroadcastManager"

/**
 * Manages the WebRTC broadcaster lifecycle:
 *  1. Opens WebSocket to signaling server
 *  2. Creates PeerConnection with camera/mic tracks
 *  3. Sends SDP offer, receives answer, exchanges ICE candidates
 *
 * Call [start] after construction. Call [release] when streaming stops.
 */
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

    /** EGL context shared between WebRTC encoder and the preview SurfaceViewRenderer. */
    val eglBase: EglBase = EglBase.create()

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var videoSource: VideoSource? = null
    private var audioSource: AudioSource? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var webSocket: WebSocket? = null

    /** Local video track - available synchronously after [start] returns. */
    var videoTrack: VideoTrack? = null
        private set

    /** Local audio track - available synchronously after [start] returns. */
    var audioTrack: AudioTrack? = null
        private set

    fun start() {
        initPeerConnectionFactory()
        openWebSocket()
    }

    fun release() {
        try { webSocket?.close(1000, "Stream stopped") } catch (_: Exception) {}
        try { videoCapturer?.stopCapture() } catch (_: Exception) {}
        try {
            videoCapturer?.dispose()
            videoTrack?.dispose()
            audioTrack?.dispose()
            videoSource?.dispose()
            audioSource?.dispose()
            surfaceTextureHelper?.dispose()
            peerConnection?.close()
            peerConnection?.dispose()
            peerConnectionFactory?.dispose()
            eglBase.release()
        } catch (e: Exception) { Log.e(TAG, "release error: ${e.message}") }
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
    }

    private fun createFrontCameraCapturer(): VideoCapturer? {
        val en = Camera2Enumerator(context)
        en.deviceNames.firstOrNull { en.isFrontFacing(it) }?.let { return en.createCapturer(it, null) }
        en.deviceNames.firstOrNull { en.isBackFacing(it) }?.let  { return en.createCapturer(it, null) }
        return null
    }

    private fun openWebSocket() {
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url(broadcastUrl(streamCode, jwtToken))
            .build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected as broadcaster")
                createAndSendOffer()
            }
            override fun onMessage(ws: WebSocket, text: String) {
                Log.d(TAG, "RX: $text")
                handleSignalingMessage(text)
            }
            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket error: ${t.message}")
            }
            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed [$code] $reason")
            }
        })
    }

    private fun buildPeerConnection(): PeerConnection? {
        val factory = peerConnectionFactory ?: return null
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )
        val config = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        return factory.createPeerConnection(config, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate?) {
                candidate ?: return
                val msg = JSONObject().apply {
                    put("type", "ice_candidate")
                    put("candidate", candidate.sdp)
                    put("sdpMid", candidate.sdpMid)
                    put("sdpMLineIndex", candidate.sdpMLineIndex)
                }
                webSocket?.send(msg.toString())
                Log.d(TAG, "TX ice_candidate")
            }
            override fun onSignalingChange(s: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(s: PeerConnection.IceConnectionState?) { Log.d(TAG, "ICE $s") }
            override fun onIceConnectionReceivingChange(b: Boolean) {}
            override fun onIceGatheringChange(s: PeerConnection.IceGatheringState?) {}
            override fun onIceCandidatesRemoved(c: Array<out IceCandidate>?) {}
            override fun onAddStream(s: MediaStream?) {}
            override fun onRemoveStream(s: MediaStream?) {}
            override fun onDataChannel(dc: org.webrtc.DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(r: org.webrtc.RtpReceiver?, streams: Array<out MediaStream>?) {}
        })
    }

    private fun createAndSendOffer() {
        val pc = buildPeerConnection() ?: return
        peerConnection = pc
        videoTrack?.let { pc.addTransceiver(it, RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_ONLY)) }
        audioTrack?.let { pc.addTransceiver(it, RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_ONLY)) }
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        }
        pc.createOffer(sdpObserver(
            onCreateSuccess = { sdp ->
                pc.setLocalDescription(sdpObserver(
                    onSetSuccess = {
                        webSocket?.send(JSONObject().apply {
                            put("type", "offer")
                            put("sdp", sdp!!.description)
                        }.toString())
                        Log.d(TAG, "TX offer")
                    },
                    onSetFailure = { Log.e(TAG, "setLocalDesc: $it") }
                ), sdp)
            },
            onCreateFailure = { Log.e(TAG, "createOffer: $it") }
        ), constraints)
    }

    private fun handleSignalingMessage(text: String) {
        try {
            val json = JSONObject(text)
            when (json.optString("type")) {
                "answer" -> {
                    peerConnection?.setRemoteDescription(sdpObserver(
                        onSetSuccess  = { Log.d(TAG, "Remote description set") },
                        onSetFailure  = { Log.e(TAG, "setRemoteDesc: $it") }
                    ), SessionDescription(SessionDescription.Type.ANSWER, json.getString("sdp")))
                }
                "ice_candidate" -> {
                    peerConnection?.addIceCandidate(IceCandidate(
                        json.optString("sdpMid"),
                        json.optInt("sdpMLineIndex"),
                        json.getString("candidate")
                    ))
                    Log.d(TAG, "Added remote ICE candidate")
                }
                "error" -> Log.e(TAG, "Signaling error: ${json.optString("message")}")
                else    -> Log.d(TAG, "Unhandled: ${json.optString("type")}")
            }
        } catch (e: Exception) { Log.e(TAG, "handleSignalingMessage: ${e.message}") }
    }

    /** Helper to create a minimal [SdpObserver] with only the needed callbacks. */
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