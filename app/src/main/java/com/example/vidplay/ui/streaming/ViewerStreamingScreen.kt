package com.example.vidplay.ui.streaming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.example.vidplay.data.webrtc.WebRtcViewerManager
import com.example.vidplay.util.PreferenceHelper
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

// ── Viewer state machine ─────────────────────────────────────────────────────

private enum class ViewerState { CONNECTING, WATCHING, STREAM_ENDED, ERROR }

// ── Composable ───────────────────────────────────────────────────────────────

/**
 * Viewer screen for a live WebRTC stream.
 *
 * Full flow:
 *  1. Opens viewer WebSocket → server notifies broadcaster (new_viewer)
 *  2. Broadcaster sends SDP offer → viewer creates answer → sends back
 *  3. Both sides exchange ICE candidates → P2P path established
 *  4. Remote video track arrives via pc.ontrack → rendered in SurfaceViewRenderer
 *  5. If buffer drifts behind live → sendGoLive() → broadcaster flushes keyframe
 *  6. On stream_ended → shows "Stream has ended" UI
 */
@Composable
fun ViewerStreamingScreen(
    navController: NavController,
    streamCode: String,
    streamTitle: String = ""
) {
    val context = LocalContext.current
    val token   = remember { PreferenceHelper(context).token }

    // ── State ─────────────────────────────────────────────────────────────────
    var viewerState   by remember { mutableStateOf(ViewerState.CONNECTING) }
    var errorMessage  by remember { mutableStateOf("") }
    var remoteTrack   by remember { mutableStateOf<VideoTrack?>(null) }
    var manager       by remember { mutableStateOf<WebRtcViewerManager?>(null) }
    // Incrementing this key forces a fresh manager + WebSocket connection on retry
    var retryKey      by remember { mutableStateOf(0) }

    // ── Start / restart the viewer manager ───────────────────────────────────
    LaunchedEffect(streamCode, retryKey) {
        // Clean up any previous attempt
        manager?.release()
        manager = null
        remoteTrack = null
        viewerState = ViewerState.CONNECTING
        errorMessage = ""

        val mgr = WebRtcViewerManager(
            context      = context,
            streamCode   = streamCode,
            jwtToken     = token,
            onRemoteTrack = { track ->
                remoteTrack = track
                viewerState = ViewerState.WATCHING
            },
            onStreamEnded = { viewerState = ViewerState.STREAM_ENDED },
            onConnected   = { /* WS open — waiting for broadcaster offer */ },
            onError       = { msg ->
                errorMessage = msg
                viewerState  = ViewerState.ERROR
            }
        )
        mgr.start()
        manager = mgr
    }

    // ── Cleanup when composable leaves the composition ───────────────────────
    DisposableEffect(Unit) {
        onDispose {
            manager?.release()
        }
    }

    // ── Root container ────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // ── Remote video renderer — only shown while watching ─────────────────
        if (viewerState == ViewerState.WATCHING) {
            val currentManager = manager
            val currentTrack   = remoteTrack
            if (currentManager != null && currentTrack != null) {
                var sinkAdded by remember { mutableStateOf(false) }
                AndroidView(
                    factory = { ctx ->
                        SurfaceViewRenderer(ctx).apply {
                            init(currentManager.eglBase.eglBaseContext, null)
                            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                            setEnableHardwareScaler(true)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { renderer ->
                        if (!sinkAdded) {
                            currentTrack.addSink(renderer)
                            sinkAdded = true
                        }
                    }
                )
            }
        }

        // ── Top bar: back button + title + LIVE badge ─────────────────────────
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = streamTitle.ifBlank { streamCode },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                if (viewerState == ViewerState.WATCHING) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE53935), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }
        }

        // ── Connecting / loading overlay ──────────────────────────────────────
        if (viewerState == ViewerState.CONNECTING) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = Color.White)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Connecting to stream…",
                    color = Color.White,
                    fontSize = 16.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Waiting for broadcaster offer",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
        }

        // ── Stream ended overlay ──────────────────────────────────────────────
        if (viewerState == ViewerState.STREAM_ENDED) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Stream has ended",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { navController.popBackStack() }) {
                    Text("Go back")
                }
            }
        }

        // ── Error overlay ──────────────────────────────────────────────────────
        if (viewerState == ViewerState.ERROR) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Connection error",
                    color = Color(0xFFEF5350),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick  = { navController.popBackStack() },
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Back", color = Color.White)
                    }
                    Button(
                        onClick = { retryKey++ }  // triggers LaunchedEffect restart
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Retry")
                    }
                }
            }
        }
    }
}
