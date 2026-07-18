package com.example.vidplay.ui.VideoSection

import android.app.Activity
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vidplay.domain.model.Media
import com.example.vidplay.utils.MediaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun VideoGrid(navController: NavController, reloadTrigger: Int = 0) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var videos by remember { mutableStateOf<List<Media>>(emptyList()) }
    var selectedUris by remember { mutableStateOf<Set<Uri>>(emptySet()) }
    val isSelectionMode = selectedUris.isNotEmpty()

    fun reload() {
        scope.launch {
            videos = withContext(Dispatchers.IO) { MediaUtils.getVideos(context.contentResolver) }
        }
    }

    LaunchedEffect(reloadTrigger) { reload() }

    
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedUris = emptySet()
            reload()
        }
    }

    fun deleteSelected() {
        scope.launch {
            val uris = selectedUris.toList()
            val sender = withContext(Dispatchers.IO) {
                MediaUtils.deleteMediaItems(context.contentResolver, uris)
            }
            if (sender != null) {
                deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
            } else {
                selectedUris = emptySet()
                reload()
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        if (isSelectionMode) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedUris = emptySet() }) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                }
                Text(
                    text = "${selectedUris.size} selected",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { deleteSelected() }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete selected",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        } else {
            Text(
                text = "Videos",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(16.dp)
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(videos, key = { it.uri.toString() }) { video ->
                VideoItem(
                    video = video,
                    isSelected = video.uri in selectedUris,
                    onLongClick = { selectedUris = selectedUris + video.uri },
                    onClick = {
                        if (isSelectionMode) {
                            selectedUris = if (video.uri in selectedUris)
                                selectedUris - video.uri
                            else
                                selectedUris + video.uri
                        } else {
                            try {
                                val encodedUri = URLEncoder.encode(
                                    video.uri.toString(), StandardCharsets.UTF_8.toString()
                                )
                                navController.navigate("videoPlayer/$encodedUri")
                            } catch (e: Exception) {
                                Log.e("VideoGrid", "Navigation error: ${e.message}", e)
                            }
                        }
                    }
                )
            }
        }
    }
}
