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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import com.example.vidplay.ui.components.CustomOutlinedTextField
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

    
    LaunchedEffect(startStreamState) {
        if (startStreamState is StartStreamUiState.Success) {
            showStartDialog = false
            navController.navigate(Routes.LIVE_STREAM)
        } else if (startStreamState is StartStreamUiState.Error) {
            Toast.makeText(context, (startStreamState as StartStreamUiState.Error).message, Toast.LENGTH_LONG).show()
            viewModel.resetStartStreamState()
        }
    }

    
    LaunchedEffect(searchState) {
        if (searchState is SearchUiState.Empty) {
            Toast.makeText(context, "No such stream is live now", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 0.dp)) {
                CustomOutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Search streams") },
                modifier = Modifier
                    .fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false
                )
            )

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val selectedTabColor = Color(0xFF404EED)  
                val unselectedTabColor = Color(0xFF2A2A3E)  
                val selectedTextColor = Color.White
                val unselectedTextColor = Color(0xFFB5BAC1)  
                
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (selectedTab == 0) selectedTabColor else unselectedTabColor,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            selectedTab = 0
                            viewModel.onTabSelected(0)
                        }
                        .padding(vertical = 14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LiveTv,
                            contentDescription = "All Streams",
                            modifier = Modifier.size(18.dp),
                            tint = if (selectedTab == 0) selectedTextColor else unselectedTextColor
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "All Streams",
                            color = if (selectedTab == 0) selectedTextColor else unselectedTextColor,
                            fontWeight = if (selectedTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (selectedTab == 1) selectedTabColor else unselectedTabColor,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            selectedTab = 1
                            viewModel.onTabSelected(1)
                        }
                        .padding(vertical = 14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "My Streams",
                            modifier = Modifier.size(18.dp),
                            tint = if (selectedTab == 1) selectedTextColor else unselectedTextColor
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "My Streams",
                            color = if (selectedTab == 1) selectedTextColor else unselectedTextColor,
                            fontWeight = if (selectedTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            
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
                        Text(text = "No stream found", color = Color(0xFFB5BAC1))
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.searchStreams() },
                            modifier = Modifier.height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF404EED),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) { Text("Retry") }
                    }
                }
                
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
                                    Text(text = "No stream available", color = Color(0xFFB5BAC1))
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.fetchAllStreams() },
                                        modifier = Modifier.height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF404EED),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) { Text("Retry") }
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
                                    Text(text = "No stream available", color = Color(0xFFB5BAC1))
                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.fetchMyStreams() },
                                        modifier = Modifier.height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF404EED),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) { Text("Retry") }
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
                    .padding(bottom = 14.dp, end = 16.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
            
            if (showStartDialog) {
                StartStreamDialog(
                    onDismiss = { showStartDialog = false },
                    isLoading = startStreamState is StartStreamUiState.Loading,
                    onSubmit = { title, description, thumbnailUri ->
                        
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
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A3E)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(0.dp)) {
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                if (item.thumbnailUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(item.thumbnailUrl),
                        contentDescription = "thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E1E2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "No thumbnail",
                            modifier = Modifier.size(56.dp),
                            tint = Color(0xFF404EED)
                        )
                    }
                }

                
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (item.isLive) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE53935), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color.White, CircleShape)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(text = "LIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            
            Column(modifier = Modifier.padding(12.dp)) {
                
                Text(
                    text = item.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.White,
                    maxLines = 2
                )

                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        fontSize = 13.sp,
                        color = Color(0xFFB5BAC1),
                        modifier = Modifier.padding(top = 6.dp),
                        maxLines = 2
                    )
                }

                Spacer(Modifier.height(10.dp))

                
                val startDate = item.startedAt.substringBefore("T")
                val endDate = item.endedAt?.substringBefore("T") ?: "ongoing"
                val duration = item.durationSeconds?.let {
                    val h = it / 3600
                    val m = (it % 3600) / 60
                    val s = it % 60
                    if (h > 0) "${h}h ${m}m" else "${m}m ${s}s"
                } ?: "—"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E2E), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Started:", fontSize = 11.sp, color = Color(0xFF757575))
                        Text(text = startDate, fontSize = 11.sp, color = Color(0xFFB5BAC1), fontWeight = FontWeight.SemiBold)
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Duration:", fontSize = 11.sp, color = Color(0xFF757575))
                        Text(text = duration, fontSize = 11.sp, color = Color(0xFFB5BAC1), fontWeight = FontWeight.SemiBold)
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Peak Viewers:", fontSize = 11.sp, color = Color(0xFF757575))
                        Text(
                            text = formatCount(item.maxViewerCount),
                            fontSize = 11.sp,
                            color = Color(0xFF404EED),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A3E)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(0.dp)) {
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                if (item.thumbnailUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(item.thumbnailUrl),
                        contentDescription = "thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF1E1E2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "No thumbnail",
                            modifier = Modifier.size(56.dp),
                            tint = Color(0xFF404EED)
                        )
                    }
                }
                
                
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color(0xFFE53935), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color.White, CircleShape)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(text = "LIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.White,
                    maxLines = 2
                )
                
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        fontSize = 13.sp,
                        color = Color(0xFFB5BAC1),
                        modifier = Modifier.padding(top = 6.dp),
                        maxLines = 2
                    )
                }

                Spacer(Modifier.height(10.dp))

                
                val startedParts = item.startedAt.split("T")
                val date = startedParts.getOrNull(0) ?: item.startedAt
                val time = startedParts.getOrNull(1)?.removeSuffix("Z") ?: ""

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFF1E1E2E), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "Views",
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF404EED)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = formatCount(item.viewerCount),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF404EED)
                        )
                    }

                    Text(
                        text = "$date",
                        fontSize = 11.sp,
                        color = Color(0xFF757575)
                    )
                }
            }
        }
    }
}

