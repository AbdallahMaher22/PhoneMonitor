package com.example.phonemonitor

import android.net.TrafficStats
import android.os.Handler
import android.os.Looper
import java.util.Locale

class NetworkSpeedMeter {
    private var lastTotalRxBytes: Long = 0
    private var lastTimeStamp: Long = 0
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())

    @Volatile
    var speedFormatted: String = "0 KB/s"
        private set

    fun start() {
        if (isRunning) return
        isRunning = true
        lastTotalRxBytes = TrafficStats.getTotalRxBytes()
        lastTimeStamp = System.currentTimeMillis()
        scheduleUpdate()
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun scheduleUpdate() {
        if (!isRunning) return
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastTimeStamp

        if (timeDiff >= 1000 && lastTotalRxBytes != 0L) {
            val bytesDiff = currentRx - lastTotalRxBytes
            val speedKb = (bytesDiff * 1000) / (timeDiff * 1024)

            speedFormatted = if (speedKb >= 1024) {
                String.format(Locale.US, "%.1f MB/s", speedKb / 1024f)
            } else {
                "$speedKb KB/s"
            }

            lastTotalRxBytes = currentRx
            lastTimeStamp = currentTime
        }

        handler.postDelayed({ scheduleUpdate() }, 1000)
    }
}
