package com.example.vidplay.ui.streaming

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vidplay.data.webrtc.StreamingSession
import com.example.vidplay.presentation.state.StartStreamUiState
import com.example.vidplay.presentation.viewmodel.StreamViewModel
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

@Composable
fun LiveStreamingScreen(
    navController: NavController,
    viewModel: StreamViewModel = viewModel()
) {
    val startStreamState by viewModel.startStreamState.collectAsState()
    val stream = (startStreamState as? StartStreamUiState.Success)?.stream

    val context = LocalContext.current

    // ── Permission state ──────────────────────────────────────────────────────
    fun hasPermission(perm: String) =
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

    var hasCameraPermission by remember { mutableStateOf(hasPermission(Manifest.permission.CAMERA)) }
    var hasAudioPermission  by remember { mutableStateOf(hasPermission(Manifest.permission.RECORD_AUDIO)) }
    val hasPermissions = hasCameraPermission && hasAudioPermission

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasCameraPermission = results[Manifest.permission.CAMERA] == true
        hasAudioPermission  = results[Manifest.permission.RECORD_AUDIO] == true
    }

    // Request permissions on first entry
    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasAudioPermission) {
            permLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    // ── Start foreground service (idempotent) ─────────────────────────────────
    // Once both stream info and permissions are available, start the service.
    // The service creates WebRtcBroadcastManager and puts it in StreamingSession.
    var serviceStarted by remember { mutableStateOf(false) }
    LaunchedEffect(stream?.streamCode, hasPermissions) {
        if (stream != null && hasPermissions && !serviceStarted) {
            val intent = StreamingForegroundService.buildStartIntent(
                context, stream.streamCode, stream.streamKey, stream.title
            )
            context.startForegroundService(intent)
            serviceStarted = true
        }
    }

    // ── WebRTC manager from the live service ──────────────────────────────────
    val manager = StreamingSession.manager

    // ── Prevent double end-stream calls when Stop button is used ─────────────
    var streamEndedExplicitly by remember { mutableStateOf(false) }

    fun stopStream() {
        if (streamEndedExplicitly) return
        streamEndedExplicitly = true
        stream?.let { viewModel.endStream(it.streamCode) }
        context.stopService(Intent(context, StreamingForegroundService::class.java))
        viewModel.resetStartStreamState()
    }

    // Called when back-navigation removes this screen from the backstack
    DisposableEffect(Unit) {
        onDispose {
            if (!streamEndedExplicitly) {
                stream?.let { viewModel.endStream(it.streamCode) }
                context.stopService(Intent(context, StreamingForegroundService::class.java))
                viewModel.resetStartStreamState()
            }
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Column(modifier = Modifier.fillMaxSize()) {

        // TOP HALF: local camera preview via WebRTC SurfaceViewRenderer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black)
        ) {
            when {
                !hasCameraPermission || !hasAudioPermission -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Camera & microphone permissions required", color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            permLauncher.launch(
                                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                            )
                        }) { Text("Grant Permissions") }
                    }
                }
                manager != null -> {
                    AndroidView(
                        factory = { ctx ->
                            SurfaceViewRenderer(ctx).apply {
                                init(manager.eglBase.eglBaseContext, null)
                                setMirror(true)
                                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                                manager.videoTrack?.addSink(this)
                            }
                        },
                        onRelease = { sv ->
                            manager.videoTrack?.removeSink(sv)
                            sv.release()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    // LIVE badge overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .background(Color(0xFFE53935), RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("● LIVE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                else -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Starting stream…", color = Color.White)
                    }
                }
            }
        }

        Divider()

        // BOTTOM HALF: stream info + stop button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (stream != null) {
                Text(stream.title, fontWeight = FontWeight.Bold, fontSize = 20.sp)

                if (stream.description.isNotBlank()) {
                    Text(stream.description, fontSize = 14.sp, color = Color(0xFF757575))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Stream Code: ", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stream.streamCode,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    stopStream()
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53935),
                    contentColor   = Color.White
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Stop Streaming", fontWeight = FontWeight.Bold)
            }
        }
    }
}
