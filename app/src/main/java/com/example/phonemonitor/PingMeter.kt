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
    private lateinit var scheduledTask: Runnable

    fun start(intervalMs: Long = 4000) {
        if (running) return
        running = true
        scheduledTask = object : Runnable {
            override fun run() {
                if (!running) return
                executor.execute {
                    lastPingMs = measureOnce()
                }
                handler.postDelayed(this, intervalMs)
            }
        }
        handler.post(scheduledTask)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(scheduledTask)
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
