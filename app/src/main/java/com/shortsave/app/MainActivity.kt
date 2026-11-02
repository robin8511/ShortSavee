
@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.shortsave.app

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

private val Bg = Color(0xFF0D0F14)
private val Panel = Color(0xFF171A22)
private val Accent = Color(0xFF8B5CF6)

private const val BACKEND_URL =
    "https://shortsave-api.onrender.com"

private data class Episode(
    val chapterId: String,
    val number: String,
    val title: String,
    val locked: Boolean
)

private data class DownloadSnapshot(
    val status: Int,
    val downloaded: Long,
    val total: Long,
    val reason: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShortSaveScreen(this)
        }
    }
}

@Composable
fun HomeScreen(context: Context) {

    var reelShortUrl by remember { mutableStateOf("") }
    var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var selectedEpisode by remember { mutableStateOf<Episode?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }
    var bookId by remember { mutableStateOf("") }

    var busy by remember { mutableStateOf(false) }

    var status by remember {
        mutableStateOf("Paste a ReelShort link to get started.")
    }

    var details by remember { mutableStateOf("") }

    var downloadProgress by remember {
        mutableStateOf(0f)
    }

    var downloadSizeText by remember {
        mutableStateOf("")
    }

    var downloadSpeedText by remember {
        mutableStateOf("")
    }

    var downloadActive by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Bg,
            surface = Panel,
            primary = Accent,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Bg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {

                // HEADER

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Accent.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            contentDescription = "ShortSave",
                            tint = Accent,
                            modifier = Modifier
                                .padding(13.dp)
                                .size(28.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            "ShortSave",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Your video library",
                            color = Color.LightGray
                        )
                    }
                }

                // DOWNLOAD CARD

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Panel
                    ),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Text(
                            "Download episode",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            "Highest available quality · MP4",
                            color = Color.LightGray
                        )

                        OutlinedTextField(
                            value = reelShortUrl,
                            onValueChange = {
                                reelShortUrl = it
                                episodes = emptyList()
                                selectedEpisode = null
                                bookId = ""
                                details = ""
                                status = "Link updated. Load episodes to continue."
                            },
                            label = {
                                Text("Paste ReelShort link")
                            },
                            placeholder = {
                                Text("https://app.reelshort.com/...")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // LOAD EPISODES

                        Button(
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF292D38),
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF20232B),
                                disabledContentColor = Color(0xFF777D8A)
                            ),
                            onClick = {
                                scope.launch {
                                    busy = true
                                    status = "Finding your story..."
                                    details = ""
                                    episodes = emptyList()
                                    selectedEpisode = null

                                    try {
                                        val result = withContext(Dispatchers.IO) {
                                            postJson(
                                                "$BACKEND_URL/api/episodes",
                                                JSONObject().put(
                                                    "url",
                                                    reelShortUrl.trim()
                                                )
                                            )
                                        }

                                        if (!result.optBoolean("success")) {
                                            error(
                                                result.optString(
                                                    "error",
                                                    "Could not load episodes."
                                                )
                                            )
                                        }

                                        bookId = result.optString("bookId")

                                        if (bookId.isBlank()) {
                                            error("The server did not return a story ID.")
                                        }

                                        val array =
                                            result.optJSONArray("episodes")
                                                ?: error("No episode list was returned.")

                                        val loaded = mutableListOf<Episode>()

                                        for (i in 0 until array.length()) {
                                            val item = array.optJSONObject(i)
                                                ?: continue

                                            val chapter = item.optString("chapterId")

                                            if (
                                                chapter.isBlank() ||
                                                chapter == "null"
                                            ) {
                                                continue
                                            }

                                            loaded.add(
                                                Episode(
                                                    chapterId = chapter,
                                                    number = item.optString(
                                                        "episode",
                                                        "?"
                                                    ),
                                                    title = item.optString(
                                                        "title",
                                                        "Episode ${i + 1}"
                                                    ),
                                                    locked = item.optBoolean(
                                                        "locked",
                                                        false
                                                    )
                                                )
                                            )
                                        }

                                        if (loaded.isEmpty()) {
                                            error("No episodes found for this story.")
                                        }

                                        episodes = loaded
                                        status = "Episodes loaded successfully."
                                        details = "${loaded.size} episodes found."

                                    } catch (e: Exception) {
                                        status = "Could not load episodes"
                                        details = e.message ?: "Please try again."
                                    } finally {
                                        busy = false
                                    }
                                }
                            },
                            enabled = !busy && reelShortUrl.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Text("Load episodes")
                        }

                        // EPISODE SELECTOR

                        if (episodes.isNotEmpty()) {

                            Text(
                                "Select episode",
                                color = Color.LightGray,
                                fontWeight = FontWeight.Medium
                            )

                            Box {
                                OutlinedButton(
                                    onClick = {
                                        menuExpanded = true
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        selectedEpisode?.let {
                                            if (it.locked) {
                                                "${it.title} · Locked"
                                            } else {
                                                it.title
                                            }
                                        } ?: "Choose an episode"
                                    )
                                }

                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = {
                                        menuExpanded = false
                                    },
                                    modifier = Modifier.heightIn(max = 350.dp)
                                ) {
                                    episodes.forEach { episode ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (episode.locked) {
                                                        "${episode.title} · Locked"
                                                    } else {
                                                        episode.title
                                                    }
                                                )
                                            },
                                            enabled = !episode.locked,
                                            onClick = {
                                                selectedEpisode = episode
                                                menuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // DOWNLOAD BUTTON

                        Button(
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Accent,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFF292D38),
                                disabledContentColor = Color(0xFF858997)
                            ),
                            onClick = {
                                val episode =
                                    selectedEpisode ?: return@Button

                                scope.launch {
                                    busy = true
                                    status = "Preparing your download..."
                                    details = ""
                                    downloadProgress = 0f
                                    downloadSizeText = "Preparing file..."
                                    downloadSpeedText = "Calculating speed..."
                                    downloadActive = false

                                    try {
                                        val result = withContext(Dispatchers.IO) {
                                            postJson(
                                                "$BACKEND_URL/api/download",
                                                JSONObject()
                                                    .put("bookId", bookId)
                                                    .put(
                                                        "chapterId",
                                                        episode.chapterId
                                                    )
                                            )
                                        }

                                        if (!result.optBoolean("success")) {
                                            error(
                                                result.optString(
                                                    "error",
                                                    "Download failed."
                                                )
                                            )
                                        }

                                        val fileUrl =
                                            result.optString("downloadUrl")

                                        if (
                                            fileUrl.isBlank() ||
                                            fileUrl == "null"
                                        ) {
                                            error("No MP4 file was returned.")
                                        }

                                        val fileName = result.optString(
                                            "fileName",
                                            "ShortSave-${episode.number}.mp4"
                                        ).replace(
                                            Regex("[^A-Za-z0-9._-]"),
                                            "_"
                                        )

                                        val request = DownloadManager.Request(
                                            Uri.parse(fileUrl)
                                        )
                                            .setTitle(fileName)
                                            .setDescription(
                                                "Saving authorized ReelShort episode"
                                            )
                                            .setMimeType("video/mp4")
                                            .setNotificationVisibility(
                                                DownloadManager.Request
                                                    .VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                                            )
                                            .setDestinationInExternalPublicDir(
                                                Environment.DIRECTORY_DOWNLOADS,
                                                fileName
                                            )

                                        val manager =
                                            context.getSystemService(
                                                Context.DOWNLOAD_SERVICE
                                            ) as DownloadManager

                                        val width =
                                            result.optInt("selectedWidth")

                                        val height =
                                            result.optInt("selectedHeight")

                                        details = if (
                                            width > 0 && height > 0
                                        ) {
                                            "Selected quality: ${width} × ${height}"
                                        } else {
                                            "MP4 download started."
                                        }

                                        val downloadId =
                                            manager.enqueue(request)

                                        downloadActive = true
                                        status = "Downloading your episode..."

                                        var previousBytes = 0L
                                        var previousTime =
                                            System.currentTimeMillis()

                                        // LIVE DOWNLOAD TRACKING

                                          while (true) {
                                              val snapshot = withContext(Dispatchers.IO) {
                                                  getDownloadSnapshot(manager, downloadId)
                                              }

                                              if (snapshot == null) {
                                                  status = "Download status unavailable."
                                                  break
                                              }

                                              if (snapshot.total > 0L) {
                                                  downloadProgress =
                                                      (snapshot.downloaded.toFloat() /
                                                       snapshot.total.toFloat())
                                                          .coerceIn(0f, 1f)

                                                  downloadSizeText =
                                                      "${formatSize(snapshot.downloaded)} / ${formatSize(snapshot.total)}"

                                                  val now = System.currentTimeMillis()
                                                  val elapsed = now - previousTime

                                                  if (elapsed > 0) {
                                                      val speed =
                                                          ((snapshot.downloaded - previousBytes)
                                                              .coerceAtLeast(0L) * 1000L) / elapsed

                                                      downloadSpeedText =
                                                          "${formatSize(speed)}/s"
                                                  }

                                                  previousBytes = snapshot.downloaded
                                                  previousTime = now
                                              }

                                              when (snapshot.status) {
                                                  DownloadManager.STATUS_SUCCESSFUL -> {
                                                      downloadProgress = 1f
                                                      downloadActive = false
                                                      status = "Download completed!"
                                                      saveDownload(
                                                          context,
                                                          downloadId,
                                                          "Episode ${episode.number}: ${episode.title}",
                                                          fileName
                                                      )
                                                      downloadSpeedText = "Finished"
                                                      break
                                                  }

                                                  DownloadManager.STATUS_FAILED -> {
                                                      downloadActive = false
                                                      error("Download failed. Code: ${snapshot.reason}")
                                                  }
                                              }

                                              delay(700)
                                          }

                                      } catch (e: Exception) {
                                          downloadActive = false
                                          status = "Download failed"
                                          details = e.message ?: "Please try again."
                                      } finally {
                                          busy = false
                                      }
                                  }
                              },
                              enabled = !busy &&
                                  selectedEpisode != null &&
                                  selectedEpisode?.locked == false,
                              modifier = Modifier
                                  .fillMaxWidth()
                                  .height(50.dp),
                              shape = RoundedCornerShape(15.dp)
                          ) {
                              Icon(
                                  Icons.Default.Download,
                                  contentDescription = null
                              )
                              Spacer(Modifier.width(8.dp))
                              Text("Download episode")
                          }

                          if (downloadActive || downloadProgress > 0f) {
                              LinearProgressIndicator(
                                  progress = downloadProgress,
                                  modifier = Modifier.fillMaxWidth(),
                                  color = Accent
                              )

                              Text(
                                  "${(downloadProgress * 100).toInt()}%",
                                  color = Color.White
                              )

                              Text(downloadSizeText, color = Color.LightGray)
                              Text(downloadSpeedText, color = Color.LightGray)
                          }

                          Text(status, color = Color.White)
                          if (details.isNotBlank()) {
                              Text(details, color = Color.LightGray)
                          }
                      }
                  }
              }
          }
      }
  }

  private fun getDownloadSnapshot(
      manager: DownloadManager,
      downloadId: Long
  ): DownloadSnapshot? {
      val query = DownloadManager.Query().setFilterById(downloadId)

      manager.query(query).use { cursor ->
          if (!cursor.moveToFirst()) return null

          return DownloadSnapshot(
              status = cursor.getInt(
                  cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
              ),
              downloaded = cursor.getLong(
                  cursor.getColumnIndexOrThrow(
                      DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                  )
              ),
              total = cursor.getLong(
                  cursor.getColumnIndexOrThrow(
                      DownloadManager.COLUMN_TOTAL_SIZE_BYTES
                  )
              ),
              reason = cursor.getInt(
                  cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON)
              )
          )
      }
  }

  private fun formatSize(bytes: Long): String {
      if (bytes < 0L) return "Unknown"

      val mb = bytes / (1024.0 * 1024.0)

      return if (mb >= 1024.0) {
          String.format(Locale.US, "%.2f GB", mb / 1024.0)
      } else {
          String.format(Locale.US, "%.2f MB", mb)
      }
  }

  private suspend fun postJson(
      endpoint: String,
      body: JSONObject
  ): JSONObject {
      val connection = URL(endpoint).openConnection() as HttpURLConnection

      return try {
          connection.requestMethod = "POST"
          connection.connectTimeout = 20000
          connection.readTimeout = 360000
          connection.doOutput = true
          connection.setRequestProperty("Content-Type", "application/json")

          connection.outputStream.use {
              it.write(body.toString().toByteArray(Charsets.UTF_8))
          }

          val responseCode = connection.responseCode

          val stream = if (responseCode in 200..299) {
              connection.inputStream
          } else {
              connection.errorStream
                  ?: error("Server returned HTTP $responseCode")
          }

          val response = stream.bufferedReader().use { it.readText() }
          JSONObject(response)
      } finally {
          connection.disconnect()
      }
  }
