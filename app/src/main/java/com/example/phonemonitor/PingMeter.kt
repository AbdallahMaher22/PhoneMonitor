package com.example.phonemonitor

import android.os.Handler
import android.os.Looper
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

/**
 * يقيس زمن استجابة تقريبي (Ping) عبر محاولة فتح اتصال TCP سريع لخادم موثوق
 * (نفس الفكرة التي تستخدمها أغلب تطبيقات قياس البينج بدون Root، لأن ICMP
 * الحقيقي يحتاج غالباً صلاحيات نظام). يعمل في خيط منفصل حتى لا يجمّد الواجهة.
 */
class PingMeter {

    @Volatile private var lastPingMs: Int? = null
    private var running = false
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    
    // تم تحويله إلى Nullable لتفادي الـ Crash إذا تم إيقافه قبل أن يبدأ
    private var scheduledTask: Runnable? = null

    fun start(intervalMs: Long = 4000) {
        if (running) return
        running = true
        
        val task = object : Runnable {
            override fun run() {
                if (!running) return
                executor.execute {
                    lastPingMs = measureOnce()
                }
                handler.postDelayed(this, intervalMs)
            }
        }
        scheduledTask = task
        handler.post(task)
    }

    fun stop() {
        running = false
        // إلغاء الـ Callback بأمان فقط إذا كان موجوداً
        scheduledTask?.let { handler.removeCallbacks(it) }
        scheduledTask = null
    }

    fun getLastPing(): Int? = lastPingMs

    private fun measureOnce(): Int? {
        return try {
            val start = System.currentTimeMillis()
            val socket = Socket()
            socket.connect(InetSocketAddress("8.8.8.8", 53), 2000)
            socket.close()
            (System.currentTimeMillis() - start).toInt()
        } catch (e: Exception) {
            null
        }
    }
}
