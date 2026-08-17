package com.example.phonemonitor

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

object PrefsManager {
    private const val PREFS_NAME = "phone_monitor_prefs"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun registerListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
    }

    // ---------- العناصر الظاهرة ----------

    fun isItemEnabled(context: Context, key: String, default: Boolean = true): Boolean {
        // العناصر الجديدة تكون مخفية افتراضياً كي لا تُثقل الشاشة فجأة عند التحديث
        val realDefault = if (key in listOf("battery", "temp", "ram", "cpu", "time")) default else false
        return prefs(context).getBoolean("show_$key", realDefault)
    }

    fun setItemEnabled(context: Context, key: String, enabled: Boolean) {
        prefs(context).edit().putBoolean("show_$key", enabled).apply()
    }

    // ---------- وضع الدمج / الفصل ----------

    fun isLocked(context: Context): Boolean = prefs(context).getBoolean("locked_mode", true)

    fun setLocked(context: Context, locked: Boolean) {
        prefs(context).edit().putBoolean("locked_mode", locked).apply()
    }

    // ---------- الشفافية (خلفية / كلام منفصلين) ----------

    fun getBackgroundOpacity(context: Context): Int = prefs(context).getInt("bg_opacity", 60)
    fun setBackgroundOpacity(context: Context, value: Int) {
        prefs(context).edit().putInt("bg_opacity", value).apply()
    }

    fun getTextOpacity(context: Context): Int = prefs(context).getInt("text_opacity", 100)
    fun setTextOpacity(context: Context, value: Int) {
        prefs(context).edit().putInt("text_opacity", value).apply()
    }

    fun getBackgroundColorWithAlpha(context: Context): Int {
        val alphaPercent = getBackgroundOpacity(context)
        val alpha = (alphaPercent * 255 / 100).coerceIn(0, 255)
        return Color.argb(alpha, 0, 0, 0)
    }

    // ---------- حجم الخط ----------

    fun getTextSize(context: Context): Int = prefs(context).getInt("text_size", 13)
    fun setTextSize(context: Context, value: Int) {
        prefs(context).edit().putInt("text_size", value).apply()
    }

    // ---------- لون الخط ----------

    fun getTextColorBase(context: Context): Int =
        prefs(context).getInt("text_color_base", Color.WHITE)

    fun setTextColorBase(context: Context, color: Int) {
        prefs(context).edit().putInt("text_color_base", color).apply()
    }

    fun getTextColorWithAlpha(context: Context): Int {
        val base = getTextColorBase(context)
        val alphaPercent = getTextOpacity(context)
        val alpha = (alphaPercent * 255 / 100).coerceIn(0, 255)
        return Color.argb(alpha, Color.red(base), Color.green(base), Color.blue(base))
    }

    // ---------- حدود الكلام الخارجية ----------

    fun isOutlineEnabled(context: Context): Boolean = prefs(context).getBoolean("outline_enabled", false)
    fun setOutlineEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean("outline_enabled", enabled).apply()
    }

    fun getOutlineColor(context: Context): Int = prefs(context).getInt("outline_color", Color.BLACK)
    fun setOutlineColor(context: Context, color: Int) {
        prefs(context).edit().putInt("outline_color", color).apply()
    }

    // ---------- موقع كل عنصر على حدة ----------

    fun getItemPosition(context: Context, key: String, defaultIndex: Int): Pair<Int, Int> {
        val p = prefs(context)
        val x = p.getInt("pos_${key}_x", 20)
        val y = p.getInt("pos_${key}_y", 80 + defaultIndex * getItemSpacing(context))
        return Pair(x, y)
    }

    fun setItemPosition(context: Context, key: String, x: Int, y: Int) {
        prefs(context).edit().putInt("pos_${key}_x", x).putInt("pos_${key}_y", y).apply()
    }

    fun resetAllPositions(context: Context) {
        val editor = prefs(context).edit()
        val spacing = getItemSpacing(context)
        OverlayItems.ALL.forEachIndexed { index, item ->
            editor.putInt("pos_${item.key}_x", 20)
            editor.putInt("pos_${item.key}_y", 80 + index * spacing)
        }
        editor.putLong("layout_dirty", System.currentTimeMillis())
        editor.apply()
    }

    /** يرصّ كل العناصر الظاهرة عمودياً بدءاً من زاوية معينة من الشاشة. */
    fun snapGroupToCorner(context: Context, corner: String, screenWidthPx: Int, screenHeightPx: Int) {
        val editor = prefs(context).edit()
        val spacing = getItemSpacing(context)
        val estimatedItemWidth = (220 * context.resources.displayMetrics.density).toInt()
        val estimatedItemHeight = spacing
        val enabledItems = OverlayItems.ALL.filter { isItemEnabled(context, it.key) }
        val totalHeight = enabledItems.size * estimatedItemHeight

        val startX = when (corner) {
            "top_right", "bottom_right" -> (screenWidthPx - estimatedItemWidth - 20).coerceAtLeast(20)
            "center" -> ((screenWidthPx - estimatedItemWidth) / 2).coerceAtLeast(20)
            else -> 20
        }
        val startY = when (corner) {
            "bottom_left", "bottom_right" -> (screenHeightPx - totalHeight - 80).coerceAtLeast(80)
            "center" -> ((screenHeightPx - totalHeight) / 2).coerceAtLeast(80)
            else -> 80
        }

        enabledItems.forEachIndexed { index, item ->
            editor.putInt("pos_${item.key}_x", startX)
            editor.putInt("pos_${item.key}_y", startY + index * spacing)
        }
        editor.putLong("layout_dirty", System.currentTimeMillis())
        editor.apply()
    }

    // ---------- التباعد بين العناصر ----------

    fun getItemSpacing(context: Context): Int = prefs(context).getInt("item_spacing", 90)
    fun setItemSpacing(context: Context, value: Int) {
        prefs(context).edit().putInt("item_spacing", value).apply()
    }

    // ---------- قفل الموقع (تجاهل اللمس أثناء اللعب) ----------

    fun isPositionLocked(context: Context): Boolean = prefs(context).getBoolean("position_locked", false)
    fun setPositionLocked(context: Context, locked: Boolean) {
        prefs(context).edit().putBoolean("position_locked", locked).apply()
    }

    // ---------- الطي السريع (إخفاء/إظهار كل العناصر بلمسة واحدة) ----------

    fun isCollapsed(context: Context): Boolean = prefs(context).getBoolean("bubble_collapsed", false)
    fun setCollapsed(context: Context, collapsed: Boolean) {
        prefs(context).edit().putBoolean("bubble_collapsed", collapsed).apply()
    }

    fun getBubblePosition(context: Context): Pair<Int, Int> {
        val p = prefs(context)
        return Pair(p.getInt("bubble_x", 20), p.getInt("bubble_y", 20))
    }

    fun setBubblePosition(context: Context, x: Int, y: Int) {
        prefs(context).edit().putInt("bubble_x", x).putInt("bubble_y", y).apply()
    }

    fun getBubbleOpacity(context: Context): Int = prefs(context).getInt("bubble_opacity", 90)
    fun setBubbleOpacity(context: Context, value: Int) {
        prefs(context).edit().putInt("bubble_opacity", value).apply()
    }

    fun isBubbleHidden(context: Context): Boolean = prefs(context).getBoolean("bubble_hidden", false)
    fun setBubbleHidden(context: Context, hidden: Boolean) {
        prefs(context).edit().putBoolean("bubble_hidden", hidden).apply()
    }

    // ---------- فترة التحديث ----------

    fun getUpdateInterval(context: Context): Long =
        prefs(context).getInt("update_interval", 1000).toLong()

    // ---------- اللغة ----------

    fun getLanguage(context: Context): String =
        prefs(context).getString("app_language", "ar") ?: "ar"

    fun setLanguage(context: Context, lang: String) {
        prefs(context).edit().putString("app_language", lang).apply()
    }

    // ---------- الوضع الليلي ----------
    // القيم الممكنة: "light" / "dark" / "system"

    fun getThemeMode(context: Context): String =
        prefs(context).getString("theme_mode", "system") ?: "system"

    fun setThemeMode(context: Context, mode: String) {
        prefs(context).edit().putString("theme_mode", mode).apply()
    }
}
