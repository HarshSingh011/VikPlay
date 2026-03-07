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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.vidplay.presentation.state.StartStreamUiState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import android.widget.Toast
import coil.compose.rememberAsyncImagePainter
import com.example.vidplay.Navigation.Routes
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import com.example.vidplay.domain.model.MyStream
import com.example.vidplay.domain.model.Stream
import com.example.vidplay.presentation.state.MyStreamUiState
import com.example.vidplay.presentation.state.SearchUiState
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
    navController: NavController = rememberNavController(),
    viewModel: StreamViewModel = viewModel()
) {
    val allStreamsState by viewModel.allStreamsState.collectAsState()
    val myStreamsState  by viewModel.myStreamsState.collectAsState()
    val searchQuery    by viewModel.searchQuery.collectAsState()
    val searchState    by viewModel.searchState.collectAsState()
    val startStreamState by viewModel.startStreamState.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var showStartDialog by remember { mutableStateOf(false) }

    // Navigate to live stream screen when stream starts successfully
    LaunchedEffect(startStreamState) {
        if (startStreamState is StartStreamUiState.Success) {
            showStartDialog = false
            navController.navigate(Routes.LIVE_STREAM)
        } else if (startStreamState is StartStreamUiState.Error) {
            Toast.makeText(context, (startStreamState as StartStreamUiState.Error).message, Toast.LENGTH_LONG).show()
            viewModel.resetStartStreamState()
        }
    }

    // Show Toast when search returns empty
    LaunchedEffect(searchState) {
        if (searchState is SearchUiState.Empty) {
            Toast.makeText(context, "No such stream is live now", Toast.LENGTH_SHORT).show()
        }
    }

    // Bottom nav items for switching between app sections
    data class NavItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val route: String)
    val bottomNavItems = listOf(
        NavItem("Videos",    Icons.Default.VideoLibrary, Routes.PAGE1),
        NavItem("Streaming", Icons.Default.LiveTv,        Routes.STREAMING)
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEachIndexed { index, item ->
                    val isSelected = item.route == Routes.STREAMING
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                navController.navigate(item.route) {
                                    popUpTo(Routes.STREAMING) { inclusive = true }
                                }
                            }
                        },
                        icon  = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 0.dp)) {
                val selectedColor   = Color(0xFF2196F3)
                val unselectedColor = Color(0xFFEEEEEE)


            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFFFFFFF)),
                shape = RoundedCornerShape(24.dp),
                label = { Text("Search streams") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboardController?.hide()
                        viewModel.searchStreams()
                    }
                )
            )

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (selectedTab == 0) selectedColor else Color.Transparent,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable {
                                selectedTab = 0
                                viewModel.onTabSelected(0)
                            }
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
                            .clickable {
                                selectedTab = 1
                                viewModel.onTabSelected(1)
                            }
                            .padding(vertical = 10.dp),
                    ) {
                        Text(
                            text = "My Streams",
                            color = if (selectedTab == 1) Color.White else Color.Black
                        )
                    }
                }

            Spacer(Modifier.height(8.dp))

            // ---- Content area: search results take priority over tab data ----
            when (val sState = searchState) {
                is SearchUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SearchUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sState.results) { item -> MyStreamCard(item) }
                    }
                }
                is SearchUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = sState.message, color = Color.Red)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.searchStreams() }) { Text("Retry") }
                    }
                }
                // Idle or Empty → show the normal tab content
                SearchUiState.Idle, SearchUiState.Empty -> {
                    if (selectedTab == 0) {
                        when (val state = allStreamsState) {
                            is StreamUiState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                            is StreamUiState.Error -> {
                                Column(
                                    modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = state.message, color = Color.Red)
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = { viewModel.fetchAllStreams() }) { Text("Retry") }
                                }
                            }
                            is StreamUiState.Success -> {
                                if (state.streams.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.LiveTv,
                                                contentDescription = null,
                                                modifier = Modifier.size(64.dp),
                                                tint = Color(0xFF9E9E9E)
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            Text(
                                                text = "No live streams at the moment",
                                                color = Color(0xFF9E9E9E),
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(state.streams) { item ->
                                            StreamCard(
                                                item = item,
                                                onClick = {
                                                    val encodedTitle = URLEncoder.encode(
                                                        item.title,
                                                        StandardCharsets.UTF_8.toString()
                                                    )
                                                    navController.navigate(
                                                        "viewStream/${item.streamCode}?streamTitle=$encodedTitle"
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        when (val state = myStreamsState) {
                            is MyStreamUiState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                            is MyStreamUiState.Error -> {
                                Column(
                                    modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = state.message, color = Color.Red)
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = { viewModel.fetchMyStreams() }) { Text("Retry") }
                                }
                            }
                            is MyStreamUiState.Success -> {
                                if (state.streams.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.VideoLibrary,
                                                contentDescription = null,
                                                modifier = Modifier.size(64.dp),
                                                tint = Color(0xFF9E9E9E)
                                            )
                                            Spacer(Modifier.height(12.dp))
                                            Text(
                                                text = "No stream history yet",
                                                color = Color(0xFF9E9E9E),
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                } else {
                                    MyStreamShownScreen(
                                        streams = state.streams,
                                        query = searchQuery
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
                onClick = { showStartDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp, end = 16.dp),  // above bottom nav
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
            
            if (showStartDialog) {
                StartStreamDialog(
                    onDismiss = { showStartDialog = false },
                    isLoading = startStreamState is StartStreamUiState.Loading,
                    onSubmit = { title, description, thumbnailUri ->
                        // Only pass thumbnail if it's already a remote URL; local URIs can't be sent as-is
                        val remoteUrl = if (thumbnailUri != null && thumbnailUri.startsWith("http")) thumbnailUri else null
                        viewModel.startStream(title, description, remoteUrl)
                    }
                )
            }
        }
    }
}

@Composable
fun MyStreamCard(item: MyStream) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Thumbnail or placeholder
            if (item.thumbnailUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(item.thumbnailUrl),
                    contentDescription = "thumbnail",
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "No thumbnail",
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF9E9E9E)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Title + live badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                if (item.isLive) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE53935), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(text = "LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (item.description.isNotBlank()) {
                Text(text = item.description, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }

            Spacer(Modifier.height(6.dp))

            // Dates + duration + max viewers
            val startDate = item.startedAt.substringBefore("T")
            val endDate   = item.endedAt?.substringBefore("T") ?: "ongoing"
            val duration  = item.durationSeconds?.let {
                val h = it / 3600; val m = (it % 3600) / 60; val s = it % 60
                if (h > 0) "${h}h ${m}m" else "${m}m ${s}s"
            } ?: "—"

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Started: $startDate", fontSize = 12.sp, color = Color(0xFF757575))
                    Text(text = "Ended:   $endDate",   fontSize = 12.sp, color = Color(0xFF757575))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Duration: $duration",                      fontSize = 12.sp, color = Color(0xFF757575))
                    Text(text = "Peak viewers: ${formatCount(item.maxViewerCount)}", fontSize = 12.sp, color = Color(0xFF757575))
                }
            }
        }
    }
}

@Composable
fun StreamCard(item: Stream, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (item.thumbnailUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(item.thumbnailUrl),
                    contentDescription = "thumbnail",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "No thumbnail",
                        modifier = Modifier.size(48.dp),
                        tint = Color(0xFF9E9E9E)
                    )
                }
            }

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

