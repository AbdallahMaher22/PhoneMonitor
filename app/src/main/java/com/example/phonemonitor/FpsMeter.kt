package com.example.phonemonitor

import android.view.Choreographer

class FpsMeter : Choreographer.FrameCallback {

    private var isTracking = false
    private var frameCount = 0
    private var lastTimeNanos: Long = 0

    @Volatile
    var currentFps: Int = 0
        private set

    fun start() {
        if (!isTracking) {
            isTracking = true
            frameCount = 0
            lastTimeNanos = 0
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun stop() {
        isTracking = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!isTracking) return

        if (lastTimeNanos == 0L) {
            lastTimeNanos = frameTimeNanos
        } else {
            val deltaNanos = frameTimeNanos - lastTimeNanos
            frameCount++

            // حساب الإطارات المنجزة كل ثانية (1,000,000,000 نانوثانية)
            if (deltaNanos >= 1_000_000_000L) {
                currentFps = ((frameCount * 1_000_000_000L) / deltaNanos).toInt()
                frameCount = 0
                lastTimeNanos = frameTimeNanos
            }
        }

        // استدعاء الإطار التالي
        Choreographer.getInstance().postFrameCallback(this)
    }
}
