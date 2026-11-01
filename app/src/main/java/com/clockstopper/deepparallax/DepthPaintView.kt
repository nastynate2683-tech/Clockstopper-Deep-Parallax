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
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x557F35FF; style = Paint.Style.FILL }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND; color = Color.WHITE; strokeWidth = 42f }
    private var foreground = true
    private var brush = 42f
    private var lastX = 0f
    private var lastY = 0f
    private var scale = 1f
    private var left = 0f
    private var top = 0f

    fun load(uri: Uri) {
        image = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        image?.let { depth = Bitmap.createBitmap(it.width.coerceAtMost(1400), it.height.coerceAtMost(1400), Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(128,128,128)) } }
        invalidate()
    }
    fun setForeground(value: Boolean) { foreground = value; stroke.color = if(value) Color.WHITE else Color.BLACK }
    fun setBrush(value: Float) { brush = value.coerceIn(8f, 150f); stroke.strokeWidth = brush }
    fun save(path: String): Boolean {
        val map = depth ?: return false
        return try { java.io.FileOutputStream(path).use { map.compress(Bitmap.CompressFormat.PNG, 100, it) }; true } catch (_: Exception) { false }
    }
    fun hasImage() = image != null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.rgb(15,12,18))
        val src = image ?: return
        val fit = max(width.toFloat()/src.width, height.toFloat()/src.height)
        scale = fit
        val dw=src.width*fit; val dh=src.height*fit
        left=(width-dw)/2f; top=(height-dh)/2f
        canvas.drawBitmap(src,null,RectF(left,top,left+dw,top+dh),imagePaint)
        depth?.let { map ->
            val layer = Bitmap.createScaledBitmap(map, src.width, src.height, true)
            val overlay = Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 80; colorFilter = PorterDuffColorFilter(Color.CYAN,PorterDuff.Mode.SRC_ATOP) }
            // Light translucent tint signals that depth painting is active without hiding the photo.
            canvas.drawBitmap(layer,null,RectF(left,top,left+dw,top+dh),overlay)
            if(layer !== map) layer.recycle()
        }
        canvas.drawText("PAINT WHITE = NEAR  •  BLACK = FAR",16f,28f,Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.WHITE;textSize=13f;setShadowLayer(4f,1f,1f,Color.BLACK) })
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val src=image ?: return false
        val map=depth ?: return false
        val x=((event.x-left)/scale).coerceIn(0f,(src.width-1).toFloat())
        val y=((event.y-top)/scale).coerceIn(0f,(src.height-1).toFloat())
        val mx=x*(map.width-1)/max(1,src.width-1).toFloat()
        val my=y*(map.height-1)/max(1,src.height-1).toFloat()
        when(event.action) {
            MotionEvent.ACTION_DOWN -> { lastX=mx;lastY=my; drawDot(map,mx,my); invalidate(); return true }
            MotionEvent.ACTION_MOVE -> { val c=Canvas(map); stroke.strokeWidth=brush*map.width.toFloat()/src.width; c.drawLine(lastX,lastY,mx,my,stroke);lastX=mx;lastY=my;invalidate();return true }
            MotionEvent.ACTION_UP -> { drawDot(map,mx,my);invalidate();return true }
        }
        return true
    }
    private fun drawDot(map: Bitmap,x:Float,y:Float) { val p=Paint(stroke);p.style=Paint.Style.FILL;Canvas(map).drawCircle(x,y,brush*map.width/(2f*(image?.width?:map.width)),p) }
}
