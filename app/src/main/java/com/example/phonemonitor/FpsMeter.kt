package com.example.phonemonitor

import android.content.Context
import android.os.Build
import android.view.Choreographer
import android.view.WindowManager
import java.util.ArrayDeque
import kotlin.math.roundToInt

/**
 * يقيس معدل تحديث الشاشة الفعلي (VSYNC) عبر Choreographer، وهي نفس الطريقة
 * التي تستخدمها تطبيقات قياس FPS الأخرى من طرف ثالث بدون Root.
 *
 * تحسينات هذه النسخة:
 * 1) نافذة متحركة قصيرة (~500ms) بدل انتظار ثانية كاملة، فالاستجابة لأي
 *    تهنيج فعلي تبقى أسرع وأدق.
 * 2) تثبيت ذكي: لو الرقم المقاس قريب جداً من معدل تحديث الشاشة الرسمي
 *    المسجَّل في النظام (60/90/120 هرتز مثلاً)، يُعرض الرقم الرسمي النظيف
 *    بدل تذبذب القياس الطبيعي — لكن أي هبوط حقيقي وواضح يبقى ظاهراً كما هو.
 */
class FpsMeter(private val context: Context) {

    private val windowMs = 500L
    private val frameTimestamps = ArrayDeque<Long>()

    private var latestFps = 0
    private var running = false

    private val choreographer = Choreographer.getInstance()
    private val nominalRefreshRate: Float by lazy { getDeviceRefreshRate() }

    private val callback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!running) return

            val nowMs = frameTimeNanos / 1_000_000L
            frameTimestamps.addLast(nowMs)

            val cutoff = nowMs - windowMs
            while (frameTimestamps.isNotEmpty() && frameTimestamps.first() < cutoff) {
                frameTimestamps.removeFirst()
            }

            if (frameTimestamps.size >= 2) {
                val spanMs = frameTimestamps.last() - frameTimestamps.first()
                if (spanMs > 0) {
                    val rawFps = (frameTimestamps.size - 1) * 1000.0 / spanMs
                    latestFps = stabilize(rawFps)
                }
            }

            choreographer.postFrameCallback(this)
        }
    }

    private fun stabilize(rawFps: Double): Int {
        if (nominalRefreshRate > 0) {
            val nominalRounded = nominalRefreshRate.roundToInt()
            if (kotlin.math.abs(rawFps - nominalRounded) <= 2.0) {
                return nominalRounded
            }
        }
        return rawFps.roundToInt()
    }

    private fun getDeviceRefreshRate(): Float {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.defaultDisplay.refreshRate
            } else {
                val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                @Suppress("DEPRECATION")
                wm.defaultDisplay.refreshRate
            }
        } catch (e: Exception) {
            0f
        }
    }

    fun start() {
        if (running) return
        running = true
        frameTimestamps.clear()
        choreographer.postFrameCallback(callback)
    }

    fun stop() {
        running = false
        choreographer.removeFrameCallback(callback)
    }

    fun getCurrentFps(): Int = latestFps
}
