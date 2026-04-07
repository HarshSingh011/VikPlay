package com.example.vidplay.ui.VideoSection

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController

private data class StorageCategory(
    val label: String,
    val icon: ImageVector
)

private val categories = listOf(
    StorageCategory("Video",     Icons.Default.Videocam),
    StorageCategory("Music",     Icons.Default.MusicNote),
    StorageCategory("Downloads", Icons.Default.Download),
    StorageCategory("Documents", Icons.Default.Folder)
)

/** Returns the permissions required for the given tab index. */
private fun permissionsForTab(index: Int): Array<String> =
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        when (index) {
            0    -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
            1    -> arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
            else -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
        }
    } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

@Composable
fun LocalStorageScreen(navController: NavController) {
    val context = LocalContext.current
    var selectedIndex by remember { mutableStateOf(0) }
    // Incremented every time any tab is tapped, so LaunchedEffect always re-fires
    // and re-requests permission if the user previously dismissed or ignored the dialog.
    var permissionTrigger by remember { mutableStateOf(0) }

    // Incremented whenever at least one new permission is granted; children observe this to reload.
    var reloadTrigger by remember { mutableStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        results.forEach { (perm, granted) ->
            Log.d("LocalStorageScreen", "$perm granted=$granted")
        }
        if (results.values.any { it }) reloadTrigger++
    }

    // Re-check permission every time the selected tab changes OR the user taps a tab again.
    LaunchedEffect(selectedIndex, permissionTrigger) {
        val needed = permissionsForTab(selectedIndex).filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Title ──
        Text(
            text = "Local Storage",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 12.dp)
        )

        // ── Category LazyRow ──
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(categories) { index, category ->
                val selected = index == selectedIndex
                val bgColor = if (selected)
                    Color(0xFF404EED)
                else
                    Color(0xFF2A2A3E)
                val contentColor = if (selected)
                    Color.White
                else
                    Color(0xFFB5BAC1)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .clickable {
                            selectedIndex = index
                            permissionTrigger++ // always re-ask if permission is still missing
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = category.label,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = category.label,
                        color = contentColor,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Content area ──
        Box(modifier = Modifier
            .fillMaxWidth()
            .weight(1f)) {
            when (selectedIndex) {
                0 -> VideoGrid(navController = navController, reloadTrigger = reloadTrigger)
                1 -> MusicListScreen(reloadTrigger = reloadTrigger)
                3 -> DocumentListScreen()
                else -> {
                    val label = categories[selectedIndex].label
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = categories[selectedIndex].icon,
                                contentDescription = label,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "$label coming soon",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
