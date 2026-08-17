package com.example.phonemonitor

import android.net.TrafficStats

/**
 * يقيس سرعة الإنترنت الفعلية الحالية (ليس اختبار سرعة نشط) عبر قراءة إجمالي
 * البيانات المُستقبَلة من النظام (TrafficStats) وحساب الفرق بين قراءتين.
 * هذا لا يستهلك أي بيانات إضافية من باقتك، ويعكس فقط ما يجري تحميله فعلياً
 * الآن (تصفح، تحديثات تطبيقات، إلخ) — وليس اختبار سرعة نشط (Speedtest).
 */
class NetworkSpeedMeter {
    private var lastBytes = -1L
    private var lastTimeMs = 0L

    fun sampleMbps(): Double {
        val now = System.currentTimeMillis()
        val bytes = TrafficStats.getTotalRxBytes()
        if (bytes < 0) return 0.0

        if (lastBytes < 0) {
            lastBytes = bytes
            lastTimeMs = now
            return 0.0
        }

        val deltaBytes = (bytes - lastBytes).coerceAtLeast(0)
        val deltaSeconds = (now - lastTimeMs) / 1000.0
        lastBytes = bytes
        lastTimeMs = now

        if (deltaSeconds <= 0) return 0.0
        val bitsPerSecond = deltaBytes * 8 / deltaSeconds
        return bitsPerSecond / 1_000_000.0
    }
}
