package com.example.phonemonitor

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class OverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: OutlineTextView? = null
    private var params: WindowManager.LayoutParams? = null

    private val handler = Handler(Looper.getMainLooper())
    private var fpsMeter: FpsMeter? = null
    private var pingMeter: PingMeter? = null
    private var netMeter: NetworkSpeedMeter? = null
    private var statsProvider: SystemStatsProvider? = null

    companion object {
        var isRunning = false
        private const val CHANNEL_ID = "PhoneMonitorChannel"
        private const val NOTIFICATION_ID = 101
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForeground(NOTIFICATION_ID, createNotification())

        fpsMeter = FpsMeter()
        pingMeter = PingMeter()
        netMeter = NetworkSpeedMeter()
        statsProvider = SystemStatsProvider(this)

        fpsMeter?.start()
        pingMeter?.start()
        netMeter?.start()

        initOverlay()
        startStatsUpdateLoop()
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "مراقبة أداء النظام",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Phone Monitor HUD")
            .setContentText("المراقبة المباشرة للأداء قيد التشغيل")
            .setSmallIcon(R.drawable.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initOverlay() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 50
            y = 200
        }

        overlayView = OutlineTextView(this).apply {
            textSize = PrefsManager.getTextSize(this@OverlayService)
            setTextColor(PrefsManager.getTextColor(this@OverlayService))
            setOutlineColor(Color.BLACK)
            setOutlineWidth(4f)
            alpha = PrefsManager.getOpacity(this@OverlayService)
            setPadding(16, 8, 16, 8)
        }

        setupTouchAndSnapListener()
        windowManager?.addView(overlayView, params)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchAndSnapListener() {
        var initialX = 0
        var initialY = 0
        var touchStartX = 0f
        var touchStartY = 0f

        overlayView?.setOnTouchListener { _, event ->
            val p = params ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = p.x
                    initialY = p.y
                    touchStartX = event.rawX
                    touchStartY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    p.x = initialX + (event.rawX - touchStartX).toInt()
                    p.y = initialY + (event.rawY - touchStartY).toInt()
                    windowManager?.updateViewLayout(overlayView, p)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // سحب النافذة تلقائياً لأقرب حافة (Snap to edge)
                    val metrics = DisplayMetrics()
                    windowManager?.defaultDisplay?.getMetrics(metrics)
                    val screenWidth = metrics.widthPixels
                    val middle = screenWidth / 2
                    val targetX = if (p.x + (overlayView?.width ?: 0) / 2 < middle) 20 else screenWidth - (overlayView?.width ?: 0) - 20

                    animateSnap(p.x, targetX)
                    true
                }
                else -> false
            }
        }
    }

    private fun animateSnap(startX: Int, endX: Int) {
        val animator = ValueAnimator.ofInt(startX, endX)
        animator.duration = 200
        animator.addUpdateListener { animation ->
            params?.let { p ->
                p.x = animation.animatedValue as Int
                windowManager?.updateViewLayout(overlayView, p)
            }
        }
        animator.start()
    }

    private fun startStatsUpdateLoop() {
        handler.post(object : Runnable {
            override fun run() {
                if (!isRunning) return
                updateHUDText()
                handler.postDelayed(this, 500)
            }
        })
    }

    private fun updateHUDText() {
        val builder = StringBuilder()
        
        val fps = fpsMeter?.currentFps ?: 0
        val ping = pingMeter?.currentPing ?: -1
        val netSpeed = netMeter?.speedFormatted ?: ""
        val temp = statsProvider?.batteryTemp ?: 0f
        val ram = statsProvider?.ramUsagePercent ?: 0

        if (PrefsManager.isShowFps(this)) {
            builder.append("FPS: $fps  ")
        }
        if (PrefsManager.isShowPing(this) && ping >= 0) {
            builder.append("PING: ${ping}ms  ")
        }
        if (PrefsManager.isShowNetSpeed(this) && netSpeed.isNotEmpty()) {
            builder.append("NET: $netSpeed  ")
        }
        if (PrefsManager.isShowTemp(this)) {
            builder.append("TEMP: ${temp.toInt()}°C  ")
        }
        if (PrefsManager.isShowRam(this)) {
            builder.append("RAM: $ram%")
        }

        overlayView?.text = builder.toString().trim()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        fpsMeter?.stop()
        pingMeter?.stop()
        netMeter?.stop()

        if (overlayView != null && windowManager != null) {
            windowManager?.removeView(overlayView)
        }
    }
}
