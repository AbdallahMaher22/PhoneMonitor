package com.example.phonemonitor

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class OverlaySettingsActivity : AppCompatActivity() {

    private val palette = listOf(
        Color.WHITE, Color.BLACK, Color.parseColor("#F44336"), Color.parseColor("#E91E63"),
        Color.parseColor("#9C27B0"), Color.parseColor("#673AB7"), Color.parseColor("#3F51B5"),
        Color.parseColor("#2196F3"), Color.parseColor("#03A9F4"), Color.parseColor("#00BCD4"),
        Color.parseColor("#009688"), Color.parseColor("#4CAF50"), Color.parseColor("#8BC34A"),
        Color.parseColor("#CDDC39"), Color.parseColor("#FFEB3B"), Color.parseColor("#FFC107"),
        Color.parseColor("#FF9800"), Color.parseColor("#FF5722"), Color.parseColor("#795548"),
        Color.parseColor("#9E9E9E")
    )

    override fun attachBaseContext(newBase: Context) {
        val lang = PrefsManager.getLanguage(newBase)
        super.attachBaseContext(LocaleHelper.wrap(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeMode()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupItemToggles()
        setupLockedSwitch()
        setupOpacitySeekBars()
        setupTextSizeSeekBar()
        setupColorGrid(findViewById(R.id.colorGridText)) { color ->
            PrefsManager.setTextColorBase(this, color)
        }
        setupOutlineSection()
        setupColorGrid(findViewById(R.id.colorGridOutline)) { color ->
            PrefsManager.setOutlineColor(this, color)
        }
        setupThemeSection()
        setupLanguageSection()
        setupSpacingSeekBar()
        setupPositionLockSwitch()
        setupSnapButtons()
        setupBubbleSection()
        setupResetButton()
    }

    private fun applyThemeMode() {
        val mode = when (PrefsManager.getThemeMode(this)) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun setupItemToggles() {
        val container = findViewById<LinearLayout>(R.id.itemsContainer)
        for (item in OverlayItems.ALL) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL
            row.setPadding(0, 12, 0, 12)

            val label = TextView(this)
            label.text = "${item.icon}  ${item.label(this)}"
            label.textSize = 15f
            label.setTextColor(getColorCompat(R.color.primaryText))
            label.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val switch = Switch(this)
            switch.isChecked = PrefsManager.isItemEnabled(this, item.key)
            switch.setOnCheckedChangeListener { _, isChecked ->
                PrefsManager.setItemEnabled(this, item.key, isChecked)
            }

            row.addView(label)
            row.addView(switch)
            container.addView(row)
        }
    }

    private fun getColorCompat(resId: Int): Int =
        androidx.core.content.ContextCompat.getColor(this, resId)

    private fun setupLockedSwitch() {
        val switchLocked = findViewById<Switch>(R.id.switchLocked)
        switchLocked.isChecked = PrefsManager.isLocked(this)
        switchLocked.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.setLocked(this, isChecked)
        }
    }

    private fun setupOpacitySeekBars() {
        val seekBg = findViewById<SeekBar>(R.id.seekBgOpacity)
        seekBg.progress = PrefsManager.getBackgroundOpacity(this)
        seekBg.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                PrefsManager.setBackgroundOpacity(this@OverlaySettingsActivity, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val seekText = findViewById<SeekBar>(R.id.seekTextOpacity)
        seekText.progress = PrefsManager.getTextOpacity(this)
        seekText.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                PrefsManager.setTextOpacity(this@OverlaySettingsActivity, progress.coerceAtLeast(5))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupTextSizeSeekBar() {
        val seekSize = findViewById<SeekBar>(R.id.seekTextSize)
        seekSize.progress = PrefsManager.getTextSize(this)
        seekSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                PrefsManager.setTextSize(this@OverlaySettingsActivity, progress.coerceAtLeast(8))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupColorGrid(container: LinearLayout, onPick: (Int) -> Unit) {
        val sizePx = (40 * resources.displayMetrics.density).toInt()
        val marginPx = (6 * resources.displayMetrics.density).toInt()
        for (color in palette) {
            val swatch = Button(this)
            val params = LinearLayout.LayoutParams(sizePx, sizePx)
            params.setMargins(marginPx, marginPx, marginPx, marginPx)
            swatch.layoutParams = params
            swatch.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
            swatch.text = ""
            swatch.setOnClickListener { onPick(color) }
            container.addView(swatch)
        }
    }

    private fun setupOutlineSection() {
        val switchOutline = findViewById<Switch>(R.id.switchOutline)
        switchOutline.isChecked = PrefsManager.isOutlineEnabled(this)
        switchOutline.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.setOutlineEnabled(this, isChecked)
        }
    }

    private fun setupThemeSection() {
        val radioGroup = findViewById<RadioGroup>(R.id.radioTheme)
        val radioLight = findViewById<RadioButton>(R.id.radioThemeLight)
        val radioDark = findViewById<RadioButton>(R.id.radioThemeDark)
        val radioSystem = findViewById<RadioButton>(R.id.radioThemeSystem)

        when (PrefsManager.getThemeMode(this)) {
            "light" -> radioLight.isChecked = true
            "dark" -> radioDark.isChecked = true
            else -> radioSystem.isChecked = true
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radioThemeLight -> "light"
                R.id.radioThemeDark -> "dark"
                else -> "system"
            }
            PrefsManager.setThemeMode(this, mode)
            applyThemeMode()
            recreate()
        }
    }

    private fun setupLanguageSection() {
        val radioGroup = findViewById<RadioGroup>(R.id.radioLanguage)
        val radioArabic = findViewById<RadioButton>(R.id.radioArabic)
        val radioEnglish = findViewById<RadioButton>(R.id.radioEnglish)

        if (PrefsManager.getLanguage(this) == "en") {
            radioEnglish.isChecked = true
        } else {
            radioArabic.isChecked = true
        }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val lang = if (checkedId == R.id.radioEnglish) "en" else "ar"
            PrefsManager.setLanguage(this, lang)
            recreate()
        }
    }

    private fun setupResetButton() {
        findViewById<Button>(R.id.btnResetPositions).setOnClickListener {
            PrefsManager.resetAllPositions(this)
        }
    }

    private fun setupSpacingSeekBar() {
        val seekSpacing = findViewById<SeekBar>(R.id.seekSpacing)
        seekSpacing.progress = PrefsManager.getItemSpacing(this)
        seekSpacing.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                PrefsManager.setItemSpacing(this@OverlaySettingsActivity, progress.coerceAtLeast(50))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupPositionLockSwitch() {
        val switchLock = findViewById<Switch>(R.id.switchPositionLock)
        switchLock.isChecked = PrefsManager.isPositionLocked(this)
        switchLock.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.setPositionLocked(this, isChecked)
        }
    }

    private fun setupSnapButtons() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        findViewById<Button>(R.id.btnSnapTopLeft).setOnClickListener {
            PrefsManager.snapGroupToCorner(this, "top_left", screenWidth, screenHeight)
        }
        findViewById<Button>(R.id.btnSnapTopRight).setOnClickListener {
            PrefsManager.snapGroupToCorner(this, "top_right", screenWidth, screenHeight)
        }
        findViewById<Button>(R.id.btnSnapBottomLeft).setOnClickListener {
            PrefsManager.snapGroupToCorner(this, "bottom_left", screenWidth, screenHeight)
        }
        findViewById<Button>(R.id.btnSnapBottomRight).setOnClickListener {
            PrefsManager.snapGroupToCorner(this, "bottom_right", screenWidth, screenHeight)
        }
        findViewById<Button>(R.id.btnSnapCenter).setOnClickListener {
            PrefsManager.snapGroupToCorner(this, "center", screenWidth, screenHeight)
        }
    }

    private fun setupBubbleSection() {
        val seekOpacity = findViewById<SeekBar>(R.id.seekBubbleOpacity)
        seekOpacity.progress = PrefsManager.getBubbleOpacity(this)
        seekOpacity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                PrefsManager.setBubbleOpacity(this@OverlaySettingsActivity, progress.coerceAtLeast(10))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val switchHidden = findViewById<Switch>(R.id.switchBubbleHidden)
        switchHidden.isChecked = PrefsManager.isBubbleHidden(this)
        switchHidden.setOnCheckedChangeListener { _, isChecked ->
            PrefsManager.setBubbleHidden(this, isChecked)
        }
    }
}
