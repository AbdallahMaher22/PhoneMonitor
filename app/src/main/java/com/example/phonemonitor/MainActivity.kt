package com.example.phonemonitor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Context

class MainActivity : AppCompatActivity() {

    private var serviceRunning = false

    override fun attachBaseContext(newBase: Context) {
        val lang = PrefsManager.getLanguage(newBase)
        super.attachBaseContext(LocaleHelper.wrap(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeMode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val txtStatus = findViewById<TextView>(R.id.txtStatus)
        val btnPermission = findViewById<Button>(R.id.btnPermission)
        val btnToggleService = findViewById<Button>(R.id.btnToggleService)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        btnPermission.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, getString(R.string.toast_permission_granted), Toast.LENGTH_SHORT).show()
            }
        }

        btnToggleService.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, getString(R.string.toast_need_permission), Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        100
                    )
                }
            }

            if (!serviceRunning) {
                startForegroundService(Intent(this, OverlayService::class.java))
                serviceRunning = true
                txtStatus.text = getString(R.string.main_status_running)
                btnToggleService.text = getString(R.string.btn_stop)
            } else {
                stopService(Intent(this, OverlayService::class.java))
                serviceRunning = false
                txtStatus.text = getString(R.string.main_status_stopped)
                btnToggleService.text = getString(R.string.btn_start)
            }
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, OverlaySettingsActivity::class.java))
        }
    }

    private fun applyThemeMode() {
        val mode = when (PrefsManager.getThemeMode(this)) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    override fun onResume() {
        super.onResume()
        val txtStatus = findViewById<TextView>(R.id.txtStatus)
        if (!Settings.canDrawOverlays(this)) {
            txtStatus.text = getString(R.string.main_status_need_permission)
        }
    }
}
