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
import android.util.Log
import com.example.vidplay.data.model.ChatMessage
import com.example.vidplay.data.webrtc.WebRtcViewerManager
import com.example.vidplay.ui.components.ChatPanel
import com.example.vidplay.util.PreferenceHelper
import kotlin.math.abs
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

private enum class ViewerState { CONNECTING, WATCHING, STREAM_ENDED, ERROR }

@Composable
fun ViewerStreamingScreen(
    navController: NavController,
    streamCode: String,
    streamTitle: String = ""
) {
    val context = LocalContext.current
    val token   = remember { PreferenceHelper(context).token }
    val username = remember { PreferenceHelper(context).username }

    
    var viewerState   by remember { mutableStateOf(ViewerState.CONNECTING) }
    var errorMessage  by remember { mutableStateOf("") }
    var remoteTrack   by remember { mutableStateOf<VideoTrack?>(null) }
    var manager       by remember { mutableStateOf<WebRtcViewerManager?>(null) }
    var retryKey      by remember { mutableStateOf(0) }
    var chatMessages  by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }

    
    LaunchedEffect(streamCode, retryKey) {
        
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
            onConnected   = {  },
            onError       = { msg ->
                errorMessage = msg
                viewerState  = ViewerState.ERROR
            },
            onChatMessage = { senderUsername, messageText, senderRole, timestamp ->
                val incomingMsg = ChatMessage(
                    username = senderUsername,
                    message = messageText,
                    role = senderRole,
                    timestamp = timestamp
                )
                val isDuplicate = chatMessages.any {
                    it.role == incomingMsg.role &&
                        it.message == incomingMsg.message &&
                        abs(it.timestamp - incomingMsg.timestamp) <= 3000L
                }
                if (!isDuplicate) {
                    chatMessages = chatMessages + incomingMsg
                }
            }
        )
        mgr.start()
        manager = mgr
    }

    
    DisposableEffect(Unit) {
        onDispose {
            manager?.release()
        }
    }

    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        
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

        
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
                    .padding(12.dp)
                    .background(Color.Black, RoundedCornerShape(8.dp))
            ) {
                if (viewerState == ViewerState.WATCHING) {
                    val currentManager = manager
                    val currentTrack = remoteTrack
                    if (currentManager != null && currentTrack != null) {
                        var sinkAdded by remember { mutableStateOf(false) }
                        AndroidView(
                            factory = { ctx ->
                                SurfaceViewRenderer(ctx).apply {
                                    init(currentManager.eglBase.eglBaseContext, null)
                                    setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
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
                } else {
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        when (viewerState) {
                            ViewerState.CONNECTING -> {
                                CircularProgressIndicator(color = Color.White)
                                Spacer(Modifier.height(8.dp))
                                Text("Connecting…", color = Color.White, fontSize = 14.sp)
                            }
                            ViewerState.STREAM_ENDED -> {
                                Text("Stream Ended", color = Color.White, fontSize = 14.sp)
                            }
                            ViewerState.ERROR -> {
                                Text("Error", color = Color(0xFFEF5350), fontSize = 14.sp)
                            }
                            else -> {}
                        }
                    }
                }
            }

            
            ChatPanel(
                messages = chatMessages,
                onSendMessage = { text ->
                    val localMessage = ChatMessage(
                        username = username.ifBlank { "Viewer" },
                        message = text.trim(),
                        role = "viewer",
                        timestamp = System.currentTimeMillis()
                    )
                    chatMessages = chatMessages + localMessage
                    manager?.sendChatMessage(text, username)
                },
                username = username,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.85f)
            )
        }

        
        if (viewerState == ViewerState.WATCHING) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { manager?.sendGoLive() },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                ) {
                    Text("Go Live", fontSize = 12.sp)
                }
            }
        }

        
        if (viewerState == ViewerState.ERROR) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(errorMessage, color = Color(0xFFEF5350), fontSize = 13.sp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back", fontSize = 12.sp)
                    }
                    Button(
                        onClick = { retryKey++ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.width(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Retry", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
