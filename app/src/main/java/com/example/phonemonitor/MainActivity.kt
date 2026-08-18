package com.example.phonemonitor

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object AppUpdater {
    private const val GITHUB_API_URL = "https://api.github.com/repos/AbdallahMaher22/PhoneMonitor/releases/latest"

    fun checkForUpdates(context: Context) {
        thread {
            try {
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(response)
                    val latestVersion = json.getString("tag_name").replace("v", "").trim()
                    val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName

                    if (latestVersion != currentVersion) {
                        val assets = json.getJSONArray("assets")
                        var downloadUrl = json.getString("html_url")
                        if (assets.length() > 0) {
                            downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
                        }

                        Handler(Looper.getMainLooper()).post {
                            showUpdateDialog(context, latestVersion, downloadUrl)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showUpdateDialog(context: Context, newVersion: String, downloadUrl: String) {
        AlertDialog.Builder(context)
            .setTitle("تحديث جديد متاح ($newVersion)")
            .setMessage("يتوفر إصدار جديد من تطبيق PhoneMonitor. هل تريد التحديث الآن؟")
            .setPositiveButton("تحديث الآن") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
            .setNegativeButton("لاحقاً", null)
            .setCancelable(true)
            .show()
    }
}
