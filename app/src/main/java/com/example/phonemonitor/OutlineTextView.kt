package com.example.phonemonitor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.appcompat.widget.AppCompatTextView

class OutlineTextView(context: Context) : AppCompatTextView(context) {

    var outlineEnabled: Boolean = false
    var outlineColor: Int = android.graphics.Color.BLACK
    var outlineWidth: Float = 3f

    override fun onDraw(canvas: Canvas) {
        if (outlineEnabled) {
            val originalColor = currentTextColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = outlineWidth
            setTextColor(outlineColor)
            super.onDraw(canvas)

            paint.style = Paint.Style.FILL
            setTextColor(originalColor)
            super.onDraw(canvas)
        } else {
            super.onDraw(canvas)
        }
    }
}
