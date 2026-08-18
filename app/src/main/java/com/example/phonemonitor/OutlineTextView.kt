package com.example.phonemonitor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

class OutlineTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    private var outlineColor: Int = Color.BLACK
    private var outlineWidth: Float = 4f

    fun setOutlineColor(color: Int) {
        outlineColor = color
        invalidate()
    }

    fun setOutlineWidth(width: Float) {
        outlineWidth = width
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val currentTextColor = currentTextColor
        val paint = paint

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = outlineWidth
        setTextColor(outlineColor)
        super.onDraw(canvas)

        paint.style = Paint.Style.FILL
        setTextColor(currentTextColor)
        super.onDraw(canvas)
    }
}
