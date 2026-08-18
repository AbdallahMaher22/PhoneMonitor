package com.example.phonemonitor

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)
            setupUI()
        } catch (e: Throwable) {
            // في حالة حدوث أي خطأ سيظهر على الشاشة بدلاً من إغلاق التطبيق
            showCrashScreen(e)
        }
    }

    private fun showCrashScreen(e: Throwable) {
        val scrollView = ScrollView(this)
        val tv = TextView(this)
        tv.text = "حدث خطأ أثناء الفتح، يرجى تصوير الشاشة:\n\n${e.stackTraceToString()}"
        tv.setTextColor(Color.RED)
        tv.setPadding(32, 32, 32, 32)
        scrollView.addView(tv)
        setContentView(scrollView)
    }

    private fun setupUI() {
        val btnToggleService = findViewById<Button>(R.id.btnToggleService)
        val btnOpenSettings = findViewById<Button>(R.id.btnOpenSettings)
        val btnPermission = findViewById<Button>(R.id.btnPermission)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)

        fun checkOverlayPermission(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(this)
            } else {
                true
            }
        }

        fun updateUI() {
            if (checkOverlayPermission()) {
                btnPermission.visibility = android.view.View.GONE
                tvStatus.text = "الصلاحيات ممنوحة - التطبيق جاهز للتشغيل"
                tvStatus.setTextColor(Color.parseColor("#00E676"))
            } else {
                btnPermission.visibility = android.view.View.VISIBLE
                tvStatus.text = "يجب منح إذن الظهور فوق التطبيقات أولاً"
                tvStatus.setTextColor(Color.parseColor("#FF1744"))
            }
        }

        updateUI()

        btnPermission.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        }

        btnToggleService.setOnClickListener {
            if (!checkOverlayPermission()) {
                Toast.makeText(this, "يرجى منح الإذن أولاً", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            try {
                val serviceIntent = Intent(this, OverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
                Toast.makeText(this, "تم تشغيل النافذة العائمة", Toast.LENGTH_SHORT).show()
            } catch (e: Throwable) {
                Toast.makeText(this, "خطأ: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        btnOpenSettings.setOnClickListener {
            try {
                startActivity(Intent(this, OverlaySettingsActivity::class.java))
            } catch (e: Throwable) {
                Toast.makeText(this, "خطأ الإعدادات: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            val btnPermission = findViewById<Button>(R.id.btnPermission)
            val tvStatus = findViewById<TextView>(R.id.tvStatus)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                btnPermission.visibility = android.view.View.GONE
                tvStatus.text = "الصلاحيات ممنوحة - التطبيق جاهز للتشغيل"
                tvStatus.setTextColor(Color.parseColor("#00E676"))
            }
        } catch (e: Throwable) {}
    }
}
