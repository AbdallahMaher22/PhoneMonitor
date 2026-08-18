package com.example.phonemonitor

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

object PrefsManager {
    private const val PREF_NAME = "phone_monitor_prefs"

    private const val KEY_SHOW_FPS = "show_fps"
    private const val KEY_SHOW_PING = "show_ping"
    private const val KEY_SHOW_NET_SPEED = "show_net_speed"
    private const val KEY_SHOW_TEMP = "show_temp"
    private const val KEY_SHOW_RAM = "show_ram"
    private const val KEY_TEXT_SIZE = "text_size"
    private const val KEY_OPACITY = "opacity"
    private const val KEY_TEXT_COLOR = "text_color"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // المؤشرات
    fun isShowFps(context: Context): Boolean = getPrefs(context).getBoolean(KEY_SHOW_FPS, true)
    fun setShowFps(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_SHOW_FPS, value).apply()

    fun isShowPing(context: Context): Boolean = getPrefs(context).getBoolean(KEY_SHOW_PING, true)
    fun setShowPing(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_SHOW_PING, value).apply()

    fun isShowNetSpeed(context: Context): Boolean = getPrefs(context).getBoolean(KEY_SHOW_NET_SPEED, false)
    fun setShowNetSpeed(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_SHOW_NET_SPEED, value).apply()

    fun isShowTemp(context: Context): Boolean = getPrefs(context).getBoolean(KEY_SHOW_TEMP, true)
    fun setShowTemp(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_SHOW_TEMP, value).apply()

    fun isShowRam(context: Context): Boolean = getPrefs(context).getBoolean(KEY_SHOW_RAM, true)
    fun setShowRam(context: Context, value: Boolean) = getPrefs(context).edit().putBoolean(KEY_SHOW_RAM, value).apply()

    // المظهر والشفافية
    fun getTextSize(context: Context): Float = getPrefs(context).getFloat(KEY_TEXT_SIZE, 14f)
    fun setTextSize(context: Context, value: Float) = getPrefs(context).edit().putFloat(KEY_TEXT_SIZE, value).apply()

    fun getOpacity(context: Context): Float = getPrefs(context).getFloat(KEY_OPACITY, 0.9f)
    fun setOpacity(context: Context, value: Float) = getPrefs(context).edit().putFloat(KEY_OPACITY, value).apply()

    fun getTextColor(context: Context): Int = getPrefs(context).getInt(KEY_TEXT_COLOR, Color.parseColor("#00E5FF"))
    fun setTextColor(context: Context, value: Int) = getPrefs(context).edit().putInt(KEY_TEXT_COLOR, value).apply()
}
