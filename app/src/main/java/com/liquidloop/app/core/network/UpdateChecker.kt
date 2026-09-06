package com.liquidloop.app.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    private const val GITHUB_API_URL = "https://api.github.com/repos/liqtranq/liquid-loop/releases/latest"
    private const val TAG = "UpdateChecker"

    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val latestVersion: String,
        val releaseNotes: String,
        val downloadUrl: String
    )

    suspend fun checkForUpdates(currentVersion: String): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val responseString = reader.readText()
                reader.close()

                val json = JSONObject(responseString)
                val tagName = json.getString("tag_name") // e.g. "v1.0.1"
                val latestVersionStr = tagName.removePrefix("v")
                
                val releaseNotes = json.optString("body", "No release notes provided.")
                val htmlUrl = json.getString("htmlUrl".replace("Url", "_url")) // "html_url"

                // Compare versions (simple string comparison or basic semantic versioning check)
                val isUpdateAvailable = isNewerVersion(currentVersion, latestVersionStr)

                return@withContext UpdateInfo(
                    isUpdateAvailable = isUpdateAvailable,
                    latestVersion = latestVersionStr,
                    releaseNotes = releaseNotes,
                    downloadUrl = htmlUrl
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates", e)
        }
        return@withContext null
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        
        val length = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until length) {
            val c = currentParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
