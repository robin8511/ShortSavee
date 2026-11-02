@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.shortsave.app

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private enum class AppTab {
    HOME, DOWNLOADS
}

@Composable
fun ShortSaveScreen(context: Context) {
    var selectedTab by remember { mutableStateOf(AppTab.HOME) }
    var playingUri by remember { mutableStateOf<Uri?>(null) }
    var playingTitle by remember { mutableStateOf("") }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Color(0xFF0D0F14),
            surface = Color(0xFF171A22),
            primary = Color(0xFF8B5CF6),
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Scaffold(
            containerColor = Color(0xFF0D0F14),
            bottomBar = {
                if (playingUri == null) {
                    NavigationBar(
                        containerColor = Color(0xFF171A22)
                    ) {
                        NavigationBarItem(
                            selected = selectedTab == AppTab.HOME,
                            onClick = { selectedTab = AppTab.HOME },
                            icon = {
                                Icon(Icons.Default.Home, contentDescription = "Home")
                            },
                            label = { Text("Home") }
                        )

                        NavigationBarItem(
                            selected = selectedTab == AppTab.DOWNLOADS,
                            onClick = { selectedTab = AppTab.DOWNLOADS },
                            icon = {
                                Icon(Icons.Default.Download, contentDescription = "Downloads")
                            },
                            label = { Text("Downloads") }
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (playingUri != null) {
                    OfflinePlayer(
                        videoUri = playingUri!!,
                        title = playingTitle,
                        onBack = { playingUri = null }
                    )
                } else {
                    when (selectedTab) {
                        AppTab.HOME -> HomeScreen(context)
                        AppTab.DOWNLOADS -> DownloadsScreen(
                            context = context,
                            onPlay = { uri, title ->
                                playingUri = uri
                                playingTitle = title
                            }
                        )
                    }
                }
            }
        }
    }
}
