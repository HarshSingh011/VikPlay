package com.example.vidplay.ui.streaming

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.vidplay.domain.model.Stream
import com.example.vidplay.presentation.state.StreamUiState
import com.example.vidplay.presentation.viewmodel.StreamViewModel

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
fun AllStreamShownScreen(
    viewModel: StreamViewModel = viewModel()
) {
    val allStreamsState by viewModel.allStreamsState.collectAsState()
    val myStreamsState  by viewModel.myStreamsState.collectAsState()
    val searchQuery    by viewModel.searchQuery.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 0.dp)) {
            val selectedColor   = Color(0xFF2196F3)
            val unselectedColor = Color(0xFFEEEEEE)

            // Current raw state for the active tab
            val activeState = if (selectedTab == 0) allStreamsState else myStreamsState


            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
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

            Spacer(Modifier.height(8.dp))

            // ---- Content area: Loading / Error / Stream list ----
            when (val state = activeState) {
                is StreamUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is StreamUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = Color.Red)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            if (selectedTab == 0) viewModel.fetchAllStreams()
                            else viewModel.fetchMyStreams()
                        }) {
                            Text("Retry")
                        }
                    }
                }

                is StreamUiState.Success -> {
                    val filtered = viewModel.filterStreams(state.streams)
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
        }

        FloatingActionButton(
            onClick = { /* TODO: handle add stream action */ },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            shape = CircleShape
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add")
        }
    }
}

@Composable
fun StreamCard(item: Stream) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Image(
                painter = rememberAsyncImagePainter(item.thumbnailUrl),
                contentDescription = "thumbnail",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentScale = ContentScale.Crop
            )

            val startedParts = item.startedAt.split("T")
            val date = startedParts.getOrNull(0) ?: item.startedAt
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
                    Text(text = formatCount(item.viewerCount))
                    Text(text = "$date $time", modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

