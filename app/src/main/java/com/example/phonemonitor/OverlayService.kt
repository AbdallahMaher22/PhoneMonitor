package com.example.phonemonitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var localizedContext: Context

    private val itemViews = HashMap<String, OutlineTextView>()
    private val itemParams = HashMap<String, WindowManager.LayoutParams>()

    private var bubbleView: OutlineTextView? = null
    private var bubbleParams: WindowManager.LayoutParams? = null

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    private var dragInitialTouchX = 0f
    private var dragInitialTouchY = 0f
    private var dragInitialPositions: Map<String, Pair<Int, Int>> = emptyMap()
    private var dragMoved = false

    private val fpsMeter by lazy { FpsMeter(this) }
    private val pingMeter = PingMeter()
    private val netSpeedMeter = NetworkSpeedMeter()

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == null || key.startsWith("pos_") || key == "bubble_x" || key == "bubble_y") return@OnSharedPreferenceChangeListener
        handler.post {
            when {
                key == "position_locked" -> applyTouchLockToAllViews()
                key == "bubble_hidden" -> refreshBubbleVisibility()
                key == "bubble_opacity" -> applyBubbleOpacity()
                key == "layout_dirty" || key.startsWith("show_") || key == "locked_mode" || key == "app_language" || key == "bubble_collapsed" -> {
                    refreshLocalizedContext()
                    rebuildOverlay()
                }
                else -> restyleAllViews()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        refreshLocalizedContext()
        startForegroundServiceWithNotification()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        PrefsManager.registerListener(this, prefsListener)
        refreshBubbleVisibility()
        rebuildOverlay()
        startUpdatingStats()
    }

    private fun refreshLocalizedContext() {
        localizedContext = LocaleHelper.wrap(this, PrefsManager.getLanguage(this))
    }

    private fun startForegroundServiceWithNotification() {
        val channelId = "phone_monitor_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                localizedContext.getString(R.string.notif_title),
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val stopIntent = Intent(this, OverlayService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(localizedContext.getString(R.string.notif_title))
            .setContentText(localizedContext.getString(R.string.notif_text))
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .addAction(0, localizedContext.getString(R.string.notif_stop_action), stopPendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_STICKY
    }

    private fun overlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    // ---------- زر الطي/العرض السريع (فقاعة صغيرة قابلة للإخفاء والتخبّي عند الحافة) ----------

    private var bubbleDocked = false
    private var bubbleEstimatedWidth = 0

    private fun refreshBubbleVisibility() {
        if (PrefsManager.isBubbleHidden(this)) {
            removeBubbleView()
        } else if (bubbleView == null) {
            createBubbleView()
        }
    }

    private fun removeBubbleView() {
        bubbleView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
        bubbleView = null
        bubbleParams = null
    }

    private fun applyBubbleOpacity() {
        val opacity = PrefsManager.getBubbleOpacity(this) / 100f
        bubbleView?.alpha = if (bubbleDocked) opacity * 0.5f else opacity
    }

    private fun createBubbleView() {
        val bubble = OutlineTextView(this)
        bubble.text = "🎮"
        val padding = (10 * resources.displayMetrics.density).toInt()
        bubble.setPadding(padding, padding, padding, padding)
        bubble.textSize = 16f
        bubble.setTextColor(Color.WHITE)
        bubbleEstimatedWidth = (56 * resources.displayMetrics.density).toInt()

        val bg = GradientDrawable()
        bg.cornerRadius = 100f
        bg.setColor(Color.argb(160, 0, 0, 0))
        bubble.background = bg

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.LEFT
        val (x, y) = PrefsManager.getBubblePosition(this)
        params.x = x
        params.y = y

        windowManager.addView(bubble, params)
        bubbleView = bubble
        bubbleParams = params
        applyBubbleOpacity()

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false

        bubble.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 12 || abs(dy) > 12) moved = true
                    if (moved && bubbleDocked) {
                        bubbleDocked = false
                        applyBubbleOpacity()
                    }
                    params.x = initialX + dx
                    params.y = initialY + dy
                    try { windowManager.updateViewLayout(bubble, params) } catch (e: Exception) {}
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) {
                        // ضغطة سريعة: لو الفقاعة متخبّية عند الحافة، أول ضغطة تظهرها فقط
                        if (bubbleDocked) {
                            undockBubble()
                        } else {
                            PrefsManager.setCollapsed(this, !PrefsManager.isCollapsed(this))
                        }
                    } else {
                        snapBubbleToNearestEdge()
                    }
                    PrefsManager.setBubblePosition(this, params.x, params.y)
                    true
                }
                else -> false
            }
        }
    }

    /** يخبّئ الفقاعة جزئياً عند أقرب حافة يمين/يسار، مثل فقاعات ماسنجر. */
    private fun snapBubbleToNearestEdge() {
        val params = bubbleParams ?: return
        val screenWidth = resources.displayMetrics.widthPixels
        val bubbleCenter = params.x + bubbleEstimatedWidth / 2
        val dockLeft = bubbleCenter < screenWidth / 2

        params.x = if (dockLeft) {
            -(bubbleEstimatedWidth * 0.55).toInt()
        } else {
            screenWidth - (bubbleEstimatedWidth * 0.45).toInt()
        }
        bubbleDocked = true
        applyBubbleOpacity()
        bubbleView?.let {
            try { windowManager.updateViewLayout(it, params) } catch (e: Exception) {}
        }
    }

    private fun undockBubble() {
        val params = bubbleParams ?: return
        val screenWidth = resources.displayMetrics.widthPixels
        val dockedLeft = params.x < 0
        params.x = if (dockedLeft) 10 else screenWidth - bubbleEstimatedWidth - 10
        bubbleDocked = false
        applyBubbleOpacity()
        bubbleView?.let {
            try { windowManager.updateViewLayout(it, params) } catch (e: Exception) {}
        }
    }

    // ---------- بناء/إعادة بناء عناصر الإحصائيات ----------

    private fun rebuildOverlay() {
        for (v in itemViews.values) {
            try { windowManager.removeView(v) } catch (e: Exception) { /* تم إزالته مسبقاً */ }
        }
        itemViews.clear()
        itemParams.clear()

        if (!PrefsManager.isCollapsed(this)) {
            OverlayItems.ALL.forEachIndexed { index, item ->
                if (PrefsManager.isItemEnabled(this, item.key)) {
                    addItemView(item, index)
                }
            }
        }

        if (PrefsManager.isItemEnabled(this, "fps")) fpsMeter.start() else fpsMeter.stop()
        if (PrefsManager.isItemEnabled(this, "ping")) pingMeter.start() else pingMeter.stop()

        applyTouchLockToAllViews()
        updateStatsOnce()
    }

    private fun addItemView(item: OverlayItems.Item, index: Int) {
        val tv = OutlineTextView(this)
        tv.text = "${item.icon} ..."
        val padding = (10 * resources.displayMetrics.density).toInt()
        tv.setPadding(padding, padding / 2, padding, padding / 2)

        val bg = GradientDrawable()
        bg.cornerRadius = 20f
        tv.background = bg

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.LEFT
        val (x, y) = PrefsManager.getItemPosition(this, item.key, index)
        params.x = x
        params.y = y

        windowManager.addView(tv, params)
        itemViews[item.key] = tv
        itemParams[item.key] = params

        styleTextView(tv)
        enableDragging(item.key, tv, params)
    }

    // ---------- تحديث المظهر بدون إعادة إنشاء ----------

    private fun restyleAllViews() {
        for (tv in itemViews.values) {
            styleTextView(tv)
        }
    }

    private fun styleTextView(tv: OutlineTextView) {
        tv.textSize = PrefsManager.getTextSize(this).toFloat()
        tv.setTextColor(PrefsManager.getTextColorWithAlpha(this))
        tv.outlineEnabled = PrefsManager.isOutlineEnabled(this)
        tv.outlineColor = PrefsManager.getOutlineColor(this)
        (tv.background as? GradientDrawable)?.setColor(PrefsManager.getBackgroundColorWithAlpha(this))
        tv.invalidate()
    }

    // ---------- قفل الموقع: يجعل النصوص شفافة للمس (تمر اللمسة للعبة تحتها) ----------

    private fun applyTouchLockToAllViews() {
        val locked = PrefsManager.isPositionLocked(this)
        for ((key, params) in itemParams) {
            params.flags = if (locked) {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            } else {
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            }
            itemViews[key]?.let {
                try { windowManager.updateViewLayout(it, params) } catch (e: Exception) {}
            }
        }
    }

    // ---------- السحب (وضع مدموج أو منفصل) ----------

    private fun enableDragging(key: String, view: View, params: WindowManager.LayoutParams) {
        view.setOnTouchListener { _, event ->
            if (PrefsManager.isPositionLocked(this)) return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragInitialTouchX = event.rawX
                    dragInitialTouchY = event.rawY
                    dragMoved = false
                    dragInitialPositions = if (PrefsManager.isLocked(this)) {
                        itemParams.mapValues { Pair(it.value.x, it.value.y) }
                    } else {
                        mapOf(key to Pair(params.x, params.y))
                    }
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - dragInitialTouchX).toInt()
                    val dy = (event.rawY - dragInitialTouchY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) dragMoved = true
                    for ((k, initPos) in dragInitialPositions) {
                        val p = itemParams[k] ?: continue
                        p.x = initPos.first + dx
                        p.y = initPos.second + dy
                        itemViews[k]?.let {
                            try { windowManager.updateViewLayout(it, p) } catch (e: Exception) {}
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    for (k in dragInitialPositions.keys) {
                        val p = itemParams[k] ?: continue
                        PrefsManager.setItemPosition(this, k, p.x, p.y)
                    }
                    true
                }
                else -> false
            }
        }
    }

    // ---------- تحديث القيم الدورية ----------

    private lateinit var fpsUpdateRunnable: Runnable

    private fun startUpdatingStats() {
        updateRunnable = object : Runnable {
            override fun run() {
                updateStatsOnce()
                handler.postDelayed(this, PrefsManager.getUpdateInterval(this@OverlayService))
            }
        }
        handler.post(updateRunnable)

        // نص الفريمات يتحدّث بمعدل أسرع ومستقل حتى لو المستخدم اختار فترة تحديث أبطأ لباقي العناصر
        fpsUpdateRunnable = object : Runnable {
            override fun run() {
                itemViews["fps"]?.let {
                    it.text = "🎮 " + localizedContext.getString(R.string.overlay_fps, fpsMeter.getCurrentFps())
                }
                handler.postDelayed(this, 500)
            }
        }
        handler.post(fpsUpdateRunnable)
    }

    private fun updateStatsOnce() {
        val ctx = localizedContext
        itemViews["battery"]?.let {
            val battery = SystemStatsProvider.getBatteryPercent(this)
            var text = ctx.getString(R.string.overlay_battery, battery)
            if (SystemStatsProvider.isCharging(this)) text += ctx.getString(R.string.overlay_battery_charging_suffix)
            it.text = "🔋 $text"
        }
        itemViews["temp"]?.let {
            val temp = SystemStatsProvider.getBatteryTemperature(this)
            it.text = "🌡️ " + ctx.getString(R.string.overlay_temp, "%.1f".format(temp))
        }
        itemViews["ram"]?.let {
            val ram = SystemStatsProvider.getRamInfo(this)
            it.text = "🧠 " + ctx.getString(R.string.overlay_ram, ram.usedMB, ram.totalMB, ram.percent)
        }
        itemViews["cpu"]?.let {
            val freq = SystemStatsProvider.getCpuFrequencyMHz()
            val cores = SystemStatsProvider.getCpuCoreCount()
            it.text = "⚙️ " + if (freq != null) ctx.getString(R.string.overlay_cpu, freq, cores)
            else ctx.getString(R.string.overlay_cpu_unavailable)
        }
        itemViews["time"]?.let {
            it.text = "🕐 ${SystemStatsProvider.getCurrentTimeString()}"
        }
        itemViews["uptime"]?.let {
            val up = SystemStatsProvider.getUptime()
            it.text = "⏱️ " + ctx.getString(R.string.overlay_uptime, up.hours, up.minutes)
        }
        itemViews["storage"]?.let {
            val s = SystemStatsProvider.getStorageInfo()
            it.text = "💾 " + ctx.getString(R.string.overlay_storage, "%.1f".format(s.freeGB), "%.1f".format(s.totalGB))
        }
        itemViews["network"]?.let {
            it.text = "📶 ${SystemStatsProvider.getNetworkType(ctx)}"
        }
        itemViews["load"]?.let {
            val load = SystemStatsProvider.getLoadAverage()
            it.text = "📊 " + if (load != null) ctx.getString(R.string.overlay_load, load)
            else ctx.getString(R.string.overlay_load_unavailable)
        }
        itemViews["charging"]?.let {
            val charging = SystemStatsProvider.isCharging(this)
            it.text = "🔌 " + if (charging) ctx.getString(R.string.overlay_charging_yes) else ctx.getString(R.string.overlay_charging_no)
        }
        itemViews["fps"]?.let {
            it.text = "🎮 " + ctx.getString(R.string.overlay_fps, fpsMeter.getCurrentFps())
        }
        itemViews["ping"]?.let {
            val ping = pingMeter.getLastPing()
            it.text = "📡 " + if (ping != null) ctx.getString(R.string.overlay_ping, ping)
            else ctx.getString(R.string.overlay_ping_unavailable)
        }
        itemViews["netspeed"]?.let {
            val mbps = netSpeedMeter.sampleMbps()
            it.text = "🚀 " + ctx.getString(R.string.overlay_netspeed, "%.1f".format(mbps))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        handler.removeCallbacks(fpsUpdateRunnable)
        fpsMeter.stop()
        pingMeter.stop()
        PrefsManager.unregisterListener(this, prefsListener)
        for (v in itemViews.values) {
            try { windowManager.removeView(v) } catch (e: Exception) {}
        }
        bubbleView?.let {
            try { windowManager.removeView(it) } catch (e: Exception) {}
        }
    }

    companion object {
        const val ACTION_STOP = "com.example.phonemonitor.ACTION_STOP"
    }
}
