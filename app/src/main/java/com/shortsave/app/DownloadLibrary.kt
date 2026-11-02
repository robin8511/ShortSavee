package com.shortsave.app

import android.content.Context
import android.app.DownloadManager
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

data class SavedDownload(
    val downloadId: Long,
    val title: String,
    val fileName: String
)

private const val LIBRARY_PREFS = "shortsave_library"
private const val LIBRARY_KEY = "downloads"

fun saveDownload(
    context: Context,
    downloadId: Long,
    title: String,
    fileName: String
) {
    val prefs = context.getSharedPreferences(
        LIBRARY_PREFS,
        Context.MODE_PRIVATE
    )

    val existing = getSavedDownloads(context).toMutableList()

    existing.removeAll { it.downloadId == downloadId }

    existing.add(
        0,
        SavedDownload(downloadId, title, fileName)
    )

    val array = JSONArray()

    existing.forEach { item ->
        array.put(
            JSONObject()
                .put("id", item.downloadId)
                .put("title", item.title)
                .put("fileName", item.fileName)
        )
    }

    prefs.edit().putString(LIBRARY_KEY, array.toString()).apply()
}

fun getSavedDownloads(context: Context): List<SavedDownload> {
    val prefs = context.getSharedPreferences(
        LIBRARY_PREFS,
        Context.MODE_PRIVATE
    )

    val raw = prefs.getString(LIBRARY_KEY, "[]") ?: "[]"
    val array = JSONArray(raw)
    val downloads = mutableListOf<SavedDownload>()

    for (i in 0 until array.length()) {
        val item = array.optJSONObject(i) ?: continue

        downloads.add(
            SavedDownload(
                downloadId = item.optLong("id"),
                title = item.optString("title"),
                fileName = item.optString("fileName")
            )
        )
    }

    return downloads
}

fun getDownloadedVideoUri(
    context: Context,
    downloadId: Long
): Uri? {
    val manager = context.getSystemService(
        Context.DOWNLOAD_SERVICE
    ) as DownloadManager

    return manager.getUriForDownloadedFile(downloadId)
}
