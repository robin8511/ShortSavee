package com.shortsave.app

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun DownloadsScreen(
    context: Context,
    onPlay: (android.net.Uri, String) -> Unit
) {
    var downloads by remember {
        mutableStateOf(getSavedDownloads(context))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text(
            "Your Downloads",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White
        )

        Spacer(Modifier.height(16.dp))

        if (downloads.isEmpty()) {
            Text(
                "No downloads yet. Your saved episodes will appear here.",
                color = Color.LightGray
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(downloads, key = { it.downloadId }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val uri = getDownloadedVideoUri(
                                    context,
                                    item.downloadId
                                )
                                if (uri != null) {
                                    onPlay(uri, item.title)
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF171A22)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = Color(0xFF8B5CF6)
                            )

                            Spacer(Modifier.width(12.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.title,
                                    color = Color.White
                                )
                                Text(
                                    item.fileName,
                                    color = Color.LightGray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
