package com.example.phonemonitor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // فحص التحديثات تلقائياً من GitHub عند تشغيل التطبيق
        AppUpdater.checkForUpdates(this)

        val btnToggleOverlay = findViewById<Button>(R.id.btnToggleOverlay)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        btnToggleOverlay?.setOnClickListener {
            if (checkOverlayPermission()) {
                toggleService()
            } else {
                requestOverlayPermission()
            }
        }

        btnSettings?.setOnClickListener {
            startActivity(Intent(this, OverlaySettingsActivity::class.java))
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(this, "يرجى منح إذن الظهور فوق التطبيقات", Toast.LENGTH_LONG).show()
        }
    }

    private fun toggleService() {
        val intent = Intent(this, OverlayService::class.java)
        if (OverlayService.isRunning) {
            stopService(intent)
            Toast.makeText(this, "تم إيقاف المراقبة", Toast.LENGTH_SHORT).show()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "تم تشغيل المراقبة", Toast.LENGTH_SHORT).show()
        }
    }
}
