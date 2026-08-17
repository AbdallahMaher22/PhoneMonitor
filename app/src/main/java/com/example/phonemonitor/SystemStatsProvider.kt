package com.example.phonemonitor

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * يقرأ معلومات النظام المتاحة عبر واجهات أندرويد العامة (بدون Root).
 *
 * ملاحظة مهمة:
 * - نسبة استخدام المعالج (CPU) الدقيقة لكل التطبيقات غير متاحة لتطبيق عادي
 *   منذ أندرويد 8، لذلك نعرض "تردد المعالج الحالي" و"متوسط الحِمل" كتقدير بديل.
 * - لا توجد واجهة برمجية رسمية لقياس FPS أو استخدام GPU لتطبيقات أخرى
 *   (كالألعاب) بدون صلاحيات نظام/Root، لذلك هذه الميزة غير مدعومة هنا عمداً.
 */
object SystemStatsProvider {

    // ---------- البطارية ----------

    fun getBatteryPercent(context: Context): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    fun getBatteryTemperature(context: Context): Float {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)
        val tempTenths = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        return tempTenths / 10f
    }

    fun isCharging(context: Context): Boolean {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = context.registerReceiver(null, intentFilter)
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    // ---------- الرام ----------

    data class RamInfo(val usedMB: Long, val totalMB: Long, val percent: Int)

    fun getRamInfo(context: Context): RamInfo {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val totalMB = memInfo.totalMem / (1024 * 1024)
        val availMB = memInfo.availMem / (1024 * 1024)
        val usedMB = totalMB - availMB
        val percent = if (totalMB > 0) ((usedMB * 100) / totalMB).toInt() else 0
        return RamInfo(usedMB, totalMB, percent)
    }

    // ---------- المعالج (تقدير عبر التردد) ----------

    fun getCpuFrequencyMHz(): Int? {
        return try {
            var coreIndex = 0
            var totalKHz = 0L
            var count = 0
            while (true) {
                val path = "/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_cur_freq"
                val file = File(path)
                if (!file.exists()) break
                val khz = BufferedReader(FileReader(file)).use { it.readLine() }?.trim()?.toLongOrNull()
                if (khz != null) {
                    totalKHz += khz
                    count++
                }
                coreIndex++
                if (coreIndex > 32) break
            }
            if (count == 0) null else (totalKHz / count / 1000).toInt()
        } catch (e: Exception) {
            null
        }
    }

    fun getCpuCoreCount(): Int = Runtime.getRuntime().availableProcessors()

    fun getLoadAverage(): String? {
        return try {
            val line = BufferedReader(FileReader("/proc/loadavg")).use { it.readLine() }
            line?.split(" ")?.getOrNull(0)
        } catch (e: Exception) {
            null
        }
    }

    // ---------- الوقت ومدة التشغيل ----------

    fun getCurrentTimeString(): String {
        val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        return sdf.format(Date())
    }

    data class UptimeInfo(val hours: Long, val minutes: Long)

    fun getUptime(): UptimeInfo {
        val totalSeconds = SystemClock.elapsedRealtime() / 1000
        return UptimeInfo(totalSeconds / 3600, (totalSeconds % 3600) / 60)
    }

    // ---------- التخزين ----------

    data class StorageInfo(val freeGB: Double, val totalGB: Double)

    fun getStorageInfo(): StorageInfo {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBytes = stat.blockCountLong * blockSize
        val freeBytes = stat.availableBlocksLong * blockSize
        val gb = 1024.0 * 1024.0 * 1024.0
        return StorageInfo(freeBytes / gb, totalBytes / gb)
    }

    // ---------- الشبكة ----------

    fun getNetworkType(context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return context.getString(R.string.network_disconnected)
            val caps = cm.getNetworkCapabilities(network) ?: return context.getString(R.string.network_disconnected)
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> context.getString(R.string.network_wifi)
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> context.getString(R.string.network_mobile)
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> context.getString(R.string.network_ethernet)
                else -> context.getString(R.string.network_connected)
            }
        } catch (e: Exception) {
            context.getString(R.string.network_unavailable)
        }
    }
}
