package com.example.vidplay.ui.VideoSection

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vidplay.domain.model.Media
import com.example.vidplay.utils.MediaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun VideoGrid(navController: NavController) {
    val context = LocalContext.current
    val videos = remember { mutableStateOf<List<Media>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = Unit) {
        videos.value = withContext(Dispatchers.IO) {
            MediaUtils.getVideos(context.contentResolver)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
    ) {
        Text(
            text = "Videos",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(videos.value) { video ->
                VideoItem(video = video) {
                    try {
                        Log.d("VideoGrid", "Original video URI: ${video.uri}")

                        val encodedUri = URLEncoder.encode(
                            video.uri.toString(),
                            StandardCharsets.UTF_8.toString()
                        )

                        Log.d("VideoGrid", "Encoded URI: $encodedUri")

                        navController.navigate("videoPlayer/$encodedUri")
                    } catch (e: Exception) {
                        Log.e("VideoGrid", "Navigation error: ${e.message}", e)
                    }
                }
            }
        }
    }
}
