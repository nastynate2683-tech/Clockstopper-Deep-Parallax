package com.clockstopper.deepparallax

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.max
import kotlin.math.min

class ParallaxWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = ParallaxEngine()

    inner class ParallaxEngine : Engine(), SensorEventListener {
        private val prefs by lazy { getSharedPreferences("parallax", Context.MODE_PRIVATE) }
        private val sensorManager by lazy { getSystemService(Context.SENSOR_SERVICE) as SensorManager }
        private val accelerometer by lazy { sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private var visibleNow = false
        private var tiltX = 0f
        private var tiltY = 0f
        private var smoothX = 0f
        private var smoothY = 0f
        private var bitmap: Bitmap? = null
        private var loadedUri: String? = null

        override fun onVisibilityChanged(visible: Boolean) {
            visibleNow = visible
            if (visible) {
                if (prefs.getBoolean("motion_enabled", true)) accelerometer?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
                }
                loadImage()
                drawFrame()
            } else {
                sensorManager.unregisterListener(this)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            if (visibleNow) drawFrame()
        }

        private fun loadImage() {
            val value = prefs.getString("image_uri", null)
            if (value == loadedUri) return
            bitmap?.recycle()
            bitmap = null
            loadedUri = value
            if (value != null) {
                try {
                    contentResolver.openInputStream(Uri.parse(value))?.use {
                        bitmap = BitmapFactory.decodeStream(it)
                    }
                } catch (_: Exception) { bitmap = null }
            }
        }

        override fun onSensorChanged(event: SensorEvent) {
            if (!visibleNow || !prefs.getBoolean("motion_enabled", true)) return
            tiltX = ((event.values[0] / 9.8f).coerceIn(-1f, 1f))
            tiltY = ((event.values[1] / 9.8f).coerceIn(-1f, 1f))
            smoothX += (tiltX - smoothX) * 0.12f
            smoothY += (tiltY - smoothY) * 0.12f
            drawFrame()
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        private fun drawFrame() {
            if (!visibleNow) return
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    val w = canvas.width.toFloat()
                    val h = canvas.height.toFloat()
                    canvas.drawColor(Color.rgb(8, 12, 24))
                    val image = bitmap
                    if (image != null && !image.isRecycled) {
                        val scale = max(w / image.width, h / image.height)
                        val drawW = image.width * scale
                        val drawH = image.height * scale
                        val strength = if (prefs.getBoolean("motion_enabled", true)) prefs.getInt("strength", 35) / 100f else 0f
                        val range = min(w, h) * 0.045f * strength
                        val dx = smoothX * range
                        val dy = -smoothY * range
                        val dst = RectF((w-drawW)/2f + dx, (h-drawH)/2f + dy, (w+drawW)/2f + dx, (h+drawH)/2f + dy)
                        canvas.drawBitmap(image, null, dst, paint)
                        // A subtle foreground depth layer moves slightly more than the image.
                        paint.color = 0x35FFFFFF
                        paint.style = Paint.Style.STROKE
                        paint.strokeWidth = 2f
                        val inset = min(w,h) * 0.08f
                        canvas.drawRoundRect(RectF(inset + dx*1.35f, inset + dy*1.35f, w-inset + dx*1.35f, h-inset + dy*1.35f), 28f, 28f, paint)
                        paint.style = Paint.Style.FILL
                        paint.color = Color.WHITE
                    } else {
                        paint.color = Color.rgb(226, 48, 72)
                        paint.textSize = 42f
                        canvas.drawText("CLOCKSTOPPER", 32f, h*0.48f, paint)
                        paint.color = Color.LTGRAY
                        paint.textSize = 22f
                        canvas.drawText("CHOOSE AN IMAGE TO BEGIN", 34f, h*0.48f+38f, paint)
                    }
                }
            } catch (_: Exception) {
            } finally {
                if (canvas != null) surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }

        override fun onDestroy() {
            visibleNow = false
            sensorManager.unregisterListener(this)
            bitmap?.recycle()
            bitmap = null
            super.onDestroy()
        }
    }
}
