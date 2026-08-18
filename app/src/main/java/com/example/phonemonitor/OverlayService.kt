package com.example.phonemonitor

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
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var tvMetrics: OutlineTextView
    private lateinit var params: WindowManager.LayoutParams

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var fpsMeter: FpsMeter
    private lateinit var pingMeter: PingMeter
    private lateinit var netMeter: NetworkSpeedMeter
    private lateinit var statsProvider: SystemStatsProvider

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // 1. تجهيز النص الاحترافي (Outline)
        tvMetrics = OutlineTextView(this).apply {
            setOutlineColor(Color.BLACK) // لون الحدود الخارجي
            setOutlineWidth(5f) // سمك الحدود
            setTextColor(PrefsManager.getTextColor(this@OverlayService)) // اللون الداخلي
            textSize = PrefsManager.getTextSize(this@OverlayService)
            alpha = PrefsManager.getOpacity(this@OverlayService)
            setPadding(16, 16, 16, 16)
        }
        floatingView = tvMetrics

        // 2. إعدادات النافذة العائمة
        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 150

        windowManager.addView(floatingView, params)

        // 3. تفعيل السحب والالتصاق المغناطيسي
        setupDragAndSnap()

        // 4. تشغيل عدادات الأداء
        fpsMeter = FpsMeter()
        pingMeter = PingMeter()
        netMeter = NetworkSpeedMeter()
        statsProvider = SystemStatsProvider(this)

        fpsMeter.start()
        pingMeter.start()
        netMeter.start()

        handler.post(updateRunnable)
    }

    // 5. التحديث اللحظي للمؤشرات بناءً على مفاتيح الإعدادات
    private val updateRunnable = object : Runnable {
        override fun run() {
            val sb = java.lang.StringBuilder()
            
            if (PrefsManager.isShowFps(this@OverlayService)) sb.append("FPS: ${fpsMeter.currentFps}  ")
            if (PrefsManager.isShowPing(this@OverlayService)) sb.append("PING: ${pingMeter.currentPing}ms  ")
            if (PrefsManager.isShowNetSpeed(this@OverlayService)) sb.append("NET: ${netMeter.speedFormatted}  ")
            if (PrefsManager.isShowTemp(this@OverlayService)) sb.append("TEMP: ${statsProvider.batteryTemp.toInt()}°C  ")
            if (PrefsManager.isShowRam(this@OverlayService)) sb.append("RAM: ${statsProvider.ramUsagePercent}%")

            tvMetrics.text = sb.toString().trim()
            
            // تطبيق الشفافية والحجم فوراً إذا تم تغييرهم من الإعدادات
            tvMetrics.textSize = PrefsManager.getTextSize(this@OverlayService)
            tvMetrics.alpha = PrefsManager.getOpacity(this@OverlayService)

            handler.postDelayed(this, 1000) // تحديث كل ثانية
        }
    }

    private fun setupDragAndSnap() {
        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        // الالتصاق بالحواف (Snap to Edge)
                        val screenWidth = resources.displayMetrics.widthPixels
                        val middle = screenWidth / 2
                        if (params.x + (floatingView.width / 2) < middle) {
                            params.x = 0 // يلتصق باليسار
                        } else {
                            params.x = screenWidth - floatingView.width // يلتصق باليمين
                        }
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun startForegroundServiceNotification() {
        val channelId = "overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Phone Monitor Overlay", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("مراقب الأداء يعمل")
            .setContentText("النافذة العائمة قيد التشغيل")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        fpsMeter.stop()
        pingMeter.stop()
        netMeter.stop()
        handler.removeCallbacks(updateRunnable)
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}
