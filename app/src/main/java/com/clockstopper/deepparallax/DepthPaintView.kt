package com.clockstopper.deepparallax

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class DepthPaintView(context: Context) : View(context) {
    private var image: Bitmap? = null
    private var depth: Bitmap? = null
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
        strokeWidth = 42f
    }
    private val cursorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.WHITE
    }
    private var foreground = true
    private var brush = 42f
    private var lastX = 0f
    private var lastY = 0f
    private var scale = 1f
    private var left = 0f
    private var top = 0f
    private var touchX = -1f
    private var touchY = -1f

    fun load(uri: Uri) {
        image = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        image?.let { src ->
            val factor = minOf(1f, 1400f / max(src.width, src.height))
            val dw = (src.width * factor).toInt().coerceAtLeast(1)
            val dh = (src.height * factor).toInt().coerceAtLeast(1)
            depth = Bitmap.createBitmap(dw, dh, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.rgb(128, 128, 128))
            }
        }
        touchX = -1f
        touchY = -1f
        invalidate()
    }

    fun setForeground(value: Boolean) {
        foreground = value
        stroke.color = if (value) Color.WHITE else Color.BLACK
        cursorPaint.color = if (value) Color.WHITE else Color.BLACK
        invalidate()
    }

    fun setBrush(value: Float) {
        brush = value.coerceIn(8f, 150f)
        stroke.strokeWidth = brush
        invalidate()
    }

    fun save(path: String): Boolean {
        val map = depth ?: return false
        return try {
            java.io.FileOutputStream(path).use { map.compress(Bitmap.CompressFormat.PNG, 100, it) }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun hasImage() = image != null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(15, 12, 18))
        val src = image ?: return

        val fit = minOf(width.toFloat() / src.width, height.toFloat() / src.height)
        scale = fit
        val dw = src.width * fit
        val dh = src.height * fit
        left = (width - dw) / 2f
        top = (height - dh) / 2f
        val rect = RectF(left, top, left + dw, top + dh)

        canvas.drawBitmap(src, null, rect, imagePaint)

        depth?.let { map ->
            // Make the depth map unmistakable: untouched mid-gray is translucent,
            // while painted near/far regions are shown strongly over the photograph.
            val layer = Bitmap.createScaledBitmap(map, src.width, src.height, true)
            val overlay = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                alpha = 105
            }
            canvas.drawBitmap(layer, null, rect, overlay)
            if (layer !== map) layer.recycle()
        }

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 13f
            setShadowLayer(4f, 1f, 1f, Color.BLACK)
        }
        canvas.drawText("WHITE = NEAR  •  BLACK = FAR  •  GRAY = UNPAINTED", 12f, 24f, labelPaint)

        if (touchX >= 0f && touchY >= 0f) {
            val radius = brush * scale / 2f
            cursorPaint.alpha = 230
            canvas.drawCircle(touchX, touchY, radius.coerceAtLeast(6f), cursorPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val src = image ?: return true
        val map = depth ?: return true
        if (scale <= 0f || src.width <= 0 || src.height <= 0) return true

        val x = ((event.x - left) / scale).coerceIn(0f, (src.width - 1).toFloat())
        val y = ((event.y - top) / scale).coerceIn(0f, (src.height - 1).toFloat())
        val mx = x * (map.width - 1) / max(1, src.width - 1).toFloat()
        val my = y * (map.height - 1) / max(1, src.height - 1).toFloat()

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                lastX = mx
                lastY = my
                drawDot(map, mx, my)
                touchX = event.x
                touchY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val p = Paint(stroke)
                p.style = Paint.Style.STROKE
                p.strokeWidth = brush * map.width.toFloat() / src.width
                Canvas(map).drawLine(lastX, lastY, mx, my, p)
                lastX = mx
                lastY = my
                touchX = event.x
                touchY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                drawDot(map, mx, my)
                touchX = event.x
                touchY = event.y
                parent?.requestDisallowInterceptTouchEvent(false)
                invalidate()
                return true
            }
        }
        return true
    }

    private fun drawDot(map: Bitmap, x: Float, y: Float) {
        val p = Paint(stroke).apply { style = Paint.Style.FILL }
        val radius = brush * map.width.toFloat() / (2f * (image?.width ?: map.width))
        Canvas(map).drawCircle(x, y, radius.coerceAtLeast(2f), p)
    }
}
