package com.example.vidplay.ui.streaming

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.random.Random
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.rememberAsyncImagePainter

data class StreamItem(
	val stream_code: String,
	val title: String,
	val description: String,
	val user_id: Int,
	val viewer_count: Int,
	val thumbnail_url: String,
	val started_at: String
)

fun sampleStreams(count: Int = 10): List<StreamItem> {
	return List(count) { i ->
		val idx = i + 1
		StreamItem(
			stream_code = List(6) { ('A'..'Z').random() }.joinToString(""),
			title = "Epic Stream #$idx",
			description = "This is a sample description for stream #$idx. Enjoy the show!",
			user_id = Random.nextInt(1, 1000),
			viewer_count = Random.nextInt(0, 5000),
			thumbnail_url = "https://picsum.photos/seed/$idx/400/200",
			started_at = "2026-02-${(1 + (i % 28)).toString().padStart(2, '0')}T10:00:00Z"
		)
	}
}

fun formatCount(count: Int): String {
	val abs = count
	return when {
		abs >= 1_000_000_000 -> {
			val v = count / 1_000_000_000.0
			String.format("%.1fB", v).replace(".0B", "B")
		}
		abs >= 1_000_000 -> {
			val v = count / 1_000_000.0
			String.format("%.1fM", v).replace(".0M", "M")
		}
		abs >= 1_000 -> {
			val v = count / 1_000.0
			String.format("%.1fK", v).replace(".0K", "K")
		}
		else -> count.toString()
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllStreamShownScreen(streams: List<StreamItem> = sampleStreams()) {
	var query by remember { mutableStateOf("") }
	var selectedTab by remember { mutableStateOf(0) }
	val currentUserId = 42 

	Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 0.dp)) {
		// Tabs row
		val selectedColor = Color(0xFF2196F3)
		val unselectedColor = Color(0xFFEEEEEE)

		OutlinedTextField(
			value = query,
			onValueChange = { query = it },
			modifier = Modifier
				.fillMaxWidth()
				.clip(RoundedCornerShape(24.dp))
				.background(Color(0xFFFFFFFF)),
			shape = RoundedCornerShape(24.dp),
			label = { Text("Search streams") },
			leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") }
		)

		Spacer(Modifier.height(24.dp))

		Box(
			modifier = Modifier
				.fillMaxWidth()
				.background(color = unselectedColor, shape = RoundedCornerShape(24.dp))
				.padding(8.dp),
		) {
			Row(modifier = Modifier.fillMaxWidth()) {
				Box(
					contentAlignment = Alignment.Center,
					modifier = Modifier
						.weight(1f)
						.background(
							color = if (selectedTab == 0) selectedColor else Color.Transparent,
							shape = RoundedCornerShape(24.dp)
						)
						.clickable { selectedTab = 0 }
						.padding(vertical = 10.dp),
				) {
					Text(
						text = "All Streams",
						color = if (selectedTab == 0) Color.White else Color.Black
					)
				}

				Spacer(modifier = Modifier.width(8.dp))

				Box(
					contentAlignment = Alignment.Center,
					modifier = Modifier
						.weight(1f)
						.background(
							color = if (selectedTab == 1) selectedColor else Color.Transparent,
							shape = RoundedCornerShape(24.dp)
						)
						.clickable { selectedTab = 1 }
						.padding(vertical = 10.dp),
				) {
					Text(
						text = "My Streams",
						color = if (selectedTab == 1) Color.White else Color.Black
					)
				}
			}
		}
        
		val tabStreams = if (selectedTab == 0) streams else streams.filter { it.user_id == currentUserId }

		val filtered = if (query.isBlank()) tabStreams else tabStreams.filter {
			it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true)
		}

		Spacer(Modifier.height(8.dp))

		LazyColumn(
			modifier = Modifier
				.fillMaxWidth()
				.padding(bottom = 12.dp),
			verticalArrangement = Arrangement.spacedBy(8.dp)
		) {
			items(filtered) { item ->
				StreamCard(item)
			}
		}
	}
}

@Composable
fun StreamCard(item: StreamItem) {
	Card(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.cardColors()
	) {
		Column(modifier = Modifier.padding(12.dp)) {
			Image(
				painter = rememberAsyncImagePainter(item.thumbnail_url),
				contentDescription = "thumbnail",
				modifier = Modifier
					.fillMaxWidth()
					.height(160.dp),
				contentScale = ContentScale.Crop
			)

			val startedParts = item.started_at.split("T")
			val date = startedParts.getOrNull(0) ?: item.started_at
			val time = startedParts.getOrNull(1)?.removeSuffix("Z") ?: ""

			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(top = 8.dp),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically
			) {
				Column(modifier = Modifier.weight(1f)) {
					Text(text = item.title)
					Text(text = item.description, modifier = Modifier.padding(top = 4.dp))
				}

				Column(horizontalAlignment = Alignment.End) {
					Text(text = "${formatCount(item.viewer_count)}")
					Text(text = "$date $time", modifier = Modifier.padding(top = 4.dp))
				}
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
fun AllStreamShownScreenPreview() {
	val sample = listOf(
		StreamItem(
			stream_code = "A1B2C3",
			title = "Epic Gaming Stream",
			description = "Watch me play Minecraft!",
			user_id = 42,
			viewer_count = 0,
			thumbnail_url = "https://example.com/thumb.jpg",
			started_at = "2026-02-24T10:00:00Z"
		)
	)
	AllStreamShownScreen(streams = sample)
}

