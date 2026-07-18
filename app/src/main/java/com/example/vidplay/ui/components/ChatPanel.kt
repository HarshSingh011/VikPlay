package com.example.vidplay.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
            .background(Color(0xFF141A24))
            .border(1.dp, Color(0xFF2A3344))
    ) {
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A2230))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Live Chat (${messages.size})",
                color = Color(0xFF79A8FF),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Divider(color = Color(0xFF2A3344), thickness = 1.dp)

        
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(8.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No messages yet",
                            color = Color(0xFF8E97A6),
                            fontSize = 12.sp
                        )
                    }
                }
            }
            items(messages) { msg ->
                ChatMessageBubble(
                    message = msg,
                    currentUsername = username
                )
            }
        }

        Divider(color = Color(0xFF2A3344), thickness = 1.dp)

        
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
                    .height(42.dp),
                singleLine = true,
                minHeight = 42f
            )
            IconButton(
                onClick = {
                    if (messageInput.trim().isNotEmpty()) {
                        onSendMessage(messageInput.trim())
                        messageInput = ""
                    }
                },
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = Color(0xFF79A8FF)
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    currentUsername: String
) {
    val isOwnMessage = message.username.equals(currentUsername, ignoreCase = true)
    val backgroundColor = if (isOwnMessage) Color(0xFF1F3A66) else Color(0xFF252E3D)
    val titleColor = if (isOwnMessage) Color(0xFF9BC1FF) else Color(0xFFE4E8EF)
    val bodyColor = Color(0xFFE4E8EF)
    val timeColor = Color(0xFF8E97A6)

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .background(backgroundColor, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (message.username.isBlank()) "User" else message.username,
                    color = titleColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
                Text(
                    text = formatTime(message.timestamp),
                    color = timeColor,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.message,
                color = bodyColor,
                fontSize = 13.sp
            )
        }
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
