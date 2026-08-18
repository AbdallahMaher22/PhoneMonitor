package com.example.phonemonitor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(try {
            LocaleHelper.onAttach(newBase)
        } catch (e: Exception) {
            newBase
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnToggleService = findViewById<Button?>(R.id.btnToggleService)
        val btnOpenSettings = findViewById<Button?>(R.id.btnOpenSettings)
        val btnPermission = findViewById<Button?>(R.id.btnPermission)
        val tvStatus = findViewById<TextView?>(R.id.tvStatus)

        fun checkOverlayPermission(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(this)
            } else {
                true
            }
        }

        fun updateUI() {
            val hasPermission = checkOverlayPermission()
            if (hasPermission) {
                btnPermission?.visibility = android.view.View.GONE
                tvStatus?.text = "الصلاحيات ممنوحة - التطبيق جاهز للتشغيل"
                tvStatus?.setTextColor(ContextCompat.getColor(this, R.color.neon_green))
            } else {
                btnPermission?.visibility = android.view.View.VISIBLE
                tvStatus?.text = "يجب منح إذن الظهور فوق التطبيقات أولاً"
                tvStatus?.setTextColor(ContextCompat.getColor(this, R.color.neon_red))
            }
        }

        updateUI()

        btnPermission?.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        btnToggleService?.setOnClickListener {
            if (!checkOverlayPermission()) {
                Toast.makeText(this, "يرجى منح إذن النافذة العائمة أولاً", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val serviceIntent = Intent(this, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Toast.makeText(this, "تم تشغيل النافذة العائمة", Toast.LENGTH_SHORT).show()
        }

        btnOpenSettings?.setOnClickListener {
            val intent = Intent(this, OverlaySettingsActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val btnPermission = findViewById<Button?>(R.id.btnPermission)
        val tvStatus = findViewById<TextView?>(R.id.tvStatus)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                btnPermission?.visibility = android.view.View.GONE
                tvStatus?.text = "الصلاحيات ممنوحة - التطبيق جاهز للتشغيل"
                tvStatus?.setTextColor(ContextCompat.getColor(this, R.color.neon_green))
            }
        }
    }
}
