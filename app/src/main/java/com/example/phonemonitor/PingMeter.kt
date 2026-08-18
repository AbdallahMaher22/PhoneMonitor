package com.example.phonemonitor

import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread

class PingMeter {
    @Volatile
    var currentPing: Int = 0
        private set

    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())

    fun start() {
        if (isRunning) return
        isRunning = true
        schedulePing()
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun schedulePing() {
        if (!isRunning) return
        thread {
            val ping = measurePing("8.8.8.8", 53, 1000)
            if (isRunning) {
                currentPing = ping
                handler.postDelayed({ schedulePing() }, 1000)
            }
        }
    }

    private fun measurePing(host: String, port: Int, timeoutMs: Int): Int {
        return try {
            val start = System.currentTimeMillis()
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeoutMs)
            }
            (System.currentTimeMillis() - start).toInt()
        } catch (e: IOException) {
            -1
        }
    }
}
