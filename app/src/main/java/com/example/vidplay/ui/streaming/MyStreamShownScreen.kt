package com.example.vidplay.ui.streaming

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MyStreamShownScreen(
    streams: List<StreamItem>,
    query: String,
    currentUserId: Int
) {
    val myStreams = streams.filter { it.user_id == currentUserId }
    val filtered = if (query.isBlank()) myStreams else myStreams.filter {
        it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filtered) { item ->
            StreamCard(item)
        }
    }
}
