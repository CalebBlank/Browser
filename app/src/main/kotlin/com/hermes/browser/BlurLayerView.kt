package com.hermes.browser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class BlurLayerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private var blurBitmap: Bitmap? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    fun setBlurBitmap(bmp: Bitmap?) {
        blurBitmap?.recycle()
        blurBitmap = bmp
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        blurBitmap?.let {
            canvas.drawBitmap(it, null, RectF(0f, 0f, width.toFloat(), height.toFloat()), paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        blurBitmap?.recycle()
        blurBitmap = null
    }
}
