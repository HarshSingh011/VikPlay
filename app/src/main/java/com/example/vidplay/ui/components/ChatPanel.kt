package com.example.vidplay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vidplay.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatPanel(
    messages: List<ChatMessage>,
    onSendMessage: (String) -> Unit,
    username: String,
    modifier: Modifier = Modifier
) {
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color(0xFF2a2a2a), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF404EED), RoundedCornerShape(8.dp))
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E2E))
                .padding(12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Live Chat",
                color = Color(0xFF404EED),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Divider(color = Color(0xFF404EED).copy(alpha = 0.3f), thickness = 1.dp)

        // Messages
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                ChatMessageBubble(msg)
            }
        }

        Divider(color = Color(0xFF404EED).copy(alpha = 0.3f), thickness = 1.dp)

        // Input area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CustomOutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = { Text("Type message...", fontSize = 12.sp) },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                singleLine = true,
                minHeight = 40f
            )
            IconButton(
                onClick = {
                    if (messageInput.trim().isNotEmpty()) {
                        onSendMessage(messageInput.trim())
                        messageInput = ""
                    }
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color(0xFF404EED)
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(message: ChatMessage) {
    val isViewer = message.role == "viewer"
    val backgroundColor = if (isViewer) Color(0xFF404EED).copy(alpha = 0.2f) else Color(0xFFB5BAC1).copy(alpha = 0.1f)
    val textColor = if (isViewer) Color(0xFFB5BAC1) else Color.White

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = if (isViewer) Alignment.Start else Alignment.End
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message.username,
                color = if (isViewer) Color(0xFF404EED) else Color(0xFFB5BAC1),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Text(
                text = formatTime(message.timestamp),
                color = textColor.copy(alpha = 0.6f),
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = message.message,
            color = textColor,
            fontSize = 13.sp
        )
    }
}

private fun formatTime(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "now"
    }
}
