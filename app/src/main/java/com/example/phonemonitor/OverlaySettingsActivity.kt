package com.example.phonemonitor

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class OverlaySettingsActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // مفاتيح تفعيل وتعطيل المؤشرات
        val switchFps = findViewById<SwitchMaterial>(R.id.switchFps)
        val switchPing = findViewById<SwitchMaterial>(R.id.switchPing)
        val switchNetSpeed = findViewById<SwitchMaterial>(R.id.switchNetSpeed)
        val switchTemp = findViewById<SwitchMaterial>(R.id.switchTemp)
        val switchRam = findViewById<SwitchMaterial>(R.id.switchRam)

        // أشرطة التحكم في الحجم والشفافية
        val seekBarTextSize = findViewById<SeekBar>(R.id.seekBarTextSize)
        val seekBarOpacity = findViewById<SeekBar>(R.id.seekBarOpacity)
        val tvTextSizeLabel = findViewById<TextView>(R.id.tvTextSizeLabel)
        val tvOpacityLabel = findViewById<TextView>(R.id.tvOpacityLabel)

        val btnSaveSettings = findViewById<Button>(R.id.btnSaveSettings)

        // تحميل القيم المحفوظة الحالية
        switchFps.isChecked = PrefsManager.isShowFps(this)
        switchPing.isChecked = PrefsManager.isShowPing(this)
        switchNetSpeed.isChecked = PrefsManager.isShowNetSpeed(this)
        switchTemp.isChecked = PrefsManager.isShowTemp(this)
        switchRam.isChecked = PrefsManager.isShowRam(this)

        val currentSize = PrefsManager.getTextSize(this).toInt()
        val currentOpacity = (PrefsManager.getOpacity(this) * 100).toInt()

        seekBarTextSize.progress = currentSize
        tvTextSizeLabel.text = "حجم الخط: $currentSize sp"

        seekBarOpacity.progress = currentOpacity
        tvOpacityLabel.text = "درجة الشفافية: $currentOpacity%"

        // مراقبة تغيير حجم الخط
        seekBarTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = if (progress < 10) 10 else progress
                tvTextSizeLabel.text = "حجم الخط: $size sp"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // مراقبة تغيير الشفافية
        seekBarOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val opacity = if (progress < 20) 20 else progress
                tvOpacityLabel.text = "درجة الشفافية: $opacity%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // زر الحفظ والتطبيق
        btnSaveSettings.setOnClickListener {
            PrefsManager.setShowFps(this, switchFps.isChecked)
            PrefsManager.setShowPing(this, switchPing.isChecked)
            PrefsManager.setShowNetSpeed(this, switchNetSpeed.isChecked)
            PrefsManager.setShowTemp(this, switchTemp.isChecked)
            PrefsManager.setShowRam(this, switchRam.isChecked)

            val finalSize = if (seekBarTextSize.progress < 10) 10f else seekBarTextSize.progress.toFloat()
            val finalOpacity = (if (seekBarOpacity.progress < 20) 20 else seekBarOpacity.progress) / 100f

            PrefsManager.setTextSize(this, finalSize)
            PrefsManager.setOpacity(this, finalOpacity)

            Toast.makeText(this, "تم حفظ الإعدادات بنجاح", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
