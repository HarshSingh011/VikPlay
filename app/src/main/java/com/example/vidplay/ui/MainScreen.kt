package com.example.vidplay.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Call
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.vidplay.Navigation.Routes

@Composable
fun MainScreen(navController: NavController) {
    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry.value?.destination?.route ?: ""

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute.startsWith("page1") || currentRoute == "page1",
                    onClick = {
                        navController.navigate("page1") {
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.Videocam, contentDescription = "Video") },
                    label = { Text("Video") }
                )

                NavigationBarItem(
                    selected = currentRoute == Routes.STREAMING,
                    onClick = {
                        navController.navigate(Routes.STREAMING) {
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.LiveTv, contentDescription = "Stream") },
                    label = { Text("Stream") }
                )

                NavigationBarItem(
                    selected = currentRoute == Routes.CALL,
                    onClick = {
                        navController.navigate(Routes.CALL) {
                            launchSingleTop = true
                        }
                    },
                    icon = { Icon(Icons.Default.Call, contentDescription = "Call") },
                    label = { Text("Call") }
                )
            }
        }
    ) { contentPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding), contentAlignment = Alignment.Center) {
            Text(text = "Select a tab using the bottom navigation")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreen(navController = rememberNavController())
}
