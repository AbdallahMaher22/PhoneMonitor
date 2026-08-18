package com.example.phonemonitor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class OverlaySettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // تم تعديل هذا السطر ليطابق اسم الملف لديك
            setContentView(R.layout.activity_settings)
            setupSettingsUI()
        } catch (e: Exception) {
            Toast.makeText(this, "خطأ في الواجهة: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupSettingsUI() {
        // ربط السويتشات
        val swFps = findViewById<SwitchCompat>(R.id.swFps)
        val swPing = findViewById<SwitchCompat>(R.id.swPing)
        val swNet = findViewById<SwitchCompat>(R.id.swNet)
        val swTemp = findViewById<SwitchCompat>(R.id.swTemp)
        val swRam = findViewById<SwitchCompat>(R.id.swRam)
        val swBattery = findViewById<SwitchCompat>(R.id.swBattery)
        val swSnap = findViewById<SwitchCompat>(R.id.swSnap)
        val swLock = findViewById<SwitchCompat>(R.id.swLock)

        // ربط الشرايط
        val seekTextSize = findViewById<SeekBar>(R.id.seekTextSize)
        val tvTextSizeLabel = findViewById<TextView>(R.id.tvTextSizeLabel)
        val seekOpacity = findViewById<SeekBar>(R.id.seekOpacity)
        val tvOpacityLabel = findViewById<TextView>(R.id.tvOpacityLabel)
        val btnSave = findViewById<Button>(R.id.btnSave)

        // استرجاع القيم المحفوظة وعرضها
        swFps.isChecked = PrefsManager.isShowFps(this)
        swPing.isChecked = PrefsManager.isShowPing(this)
        swNet.isChecked = PrefsManager.isShowNetSpeed(this)
        swTemp.isChecked = PrefsManager.isShowTemp(this)
        swRam.isChecked = PrefsManager.isShowRam(this)
        swBattery.isChecked = PrefsManager.isShowBattery(this)
        swSnap.isChecked = PrefsManager.isSnapToEdge(this)
        swLock.isChecked = PrefsManager.isLockPosition(this)

        seekTextSize.progress = PrefsManager.getTextSize(this).toInt()
        tvTextSizeLabel.text = "حجم الخط: ${seekTextSize.progress}"

        seekOpacity.progress = (PrefsManager.getOpacity(this) * 100).toInt()
        tvOpacityLabel.text = "الشفافية: ${seekOpacity.progress}%"

        // تحديث النصوص أثناء سحب الشريط
        seekTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvTextSizeLabel.text = "حجم الخط: $progress"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvOpacityLabel.text = "الشفافية: $progress%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // زر الحفظ
        btnSave.setOnClickListener {
            PrefsManager.saveSettings(
                this, swFps.isChecked, swPing.isChecked, swNet.isChecked,
                swTemp.isChecked, swRam.isChecked, swBattery.isChecked, false, // CPU false for now
                seekTextSize.progress.toFloat(),
                seekOpacity.progress / 100f,
                swSnap.isChecked, swLock.isChecked
            )
            
            Toast.makeText(this, "تم حفظ الإعدادات بنجاح!", Toast.LENGTH_SHORT).show()
            
            // إعادة تشغيل الخدمة لتطبيق التعديلات فوراً
            val serviceIntent = Intent(this, OverlayService::class.java)
            stopService(serviceIntent)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            
            finish() // إغلاق الشاشة
        }
    }
}
