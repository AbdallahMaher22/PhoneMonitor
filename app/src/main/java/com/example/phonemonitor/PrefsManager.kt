package com.example.phonemonitor

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

object PrefsManager {
    private const val PREF_NAME = "PhoneMonitorPrefs"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // المؤشرات
    fun isShowFps(context: Context) = getPrefs(context).getBoolean("show_fps", true)
    fun isShowPing(context: Context) = getPrefs(context).getBoolean("show_ping", true)
    fun isShowNetSpeed(context: Context) = getPrefs(context).getBoolean("show_net", true)
    fun isShowTemp(context: Context) = getPrefs(context).getBoolean("show_temp", true)
    fun isShowRam(context: Context) = getPrefs(context).getBoolean("show_ram", false)
    fun isShowBattery(context: Context) = getPrefs(context).getBoolean("show_battery", false)
    fun isShowCpu(context: Context) = getPrefs(context).getBoolean("show_cpu", false)

    // المظهر والتخصيص
    fun getTextSize(context: Context) = getPrefs(context).getFloat("text_size", 14f)
    fun getOpacity(context: Context) = getPrefs(context).getFloat("opacity", 1.0f)
    fun getTextColor(context: Context) = getPrefs(context).getInt("text_color", Color.CYAN)
    fun getOutlineColor(context: Context) = getPrefs(context).getInt("outline_color", Color.BLACK)

    // السلوك
    fun isSnapToEdge(context: Context) = getPrefs(context).getBoolean("snap_to_edge", true)
    fun isLockPosition(context: Context) = getPrefs(context).getBoolean("lock_position", false)

    // دالة الحفظ الشاملة
    fun saveSettings(
        context: Context, fps: Boolean, ping: Boolean, net: Boolean, temp: Boolean,
        ram: Boolean, battery: Boolean, cpu: Boolean,
        textSize: Float, opacity: Float, snap: Boolean, lock: Boolean
    ) {
        getPrefs(context).edit().apply {
            putBoolean("show_fps", fps)
            putBoolean("show_ping", ping)
            putBoolean("show_net", net)
            putBoolean("show_temp", temp)
            putBoolean("show_ram", ram)
            putBoolean("show_battery", battery)
            putBoolean("show_cpu", cpu)
            putFloat("text_size", textSize)
            putFloat("opacity", opacity)
            putBoolean("snap_to_edge", snap)
            putBoolean("lock_position", lock)
            apply()
        }
    }

    fun saveColor(context: Context, key: String, color: Int) {
        getPrefs(context).edit().putInt(key, color).apply()
    }
}
