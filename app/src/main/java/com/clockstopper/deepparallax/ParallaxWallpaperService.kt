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
        private var depthBitmap: Bitmap? = null
        private var loadedUri: String? = null
        private var loadedDepthUri: String? = null
        private val meshColumns = 32
        private val meshRows = 48

        override fun onVisibilityChanged(visible: Boolean) {
            visibleNow = visible
            if (visible) {
                if (prefs.getBoolean("motion_enabled", true)) accelerometer?.let {
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
                }
                loadImages()
                drawFrame()
            } else sensorManager.unregisterListener(this)
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            if (visibleNow) drawFrame()
        }

        private fun decode(uriText: String?): Bitmap? {
            if (uriText == null) return null
            return try {
                contentResolver.openInputStream(Uri.parse(uriText))?.use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) { null }
        }

        private fun loadImages() {
            val imageUri = prefs.getString("image_uri", null)
            if (imageUri != loadedUri) {
                bitmap?.recycle()
                bitmap = decode(imageUri)
                loadedUri = imageUri
            }
            val depthUri = prefs.getString("depth_uri", null)
            if (depthUri != loadedDepthUri) {
                depthBitmap?.recycle()
                depthBitmap = decode(depthUri)
                loadedDepthUri = depthUri
            }
        }

        override fun onSensorChanged(event: SensorEvent) {
            if (!visibleNow || !prefs.getBoolean("motion_enabled", true)) return
            tiltX = (event.values[0] / 9.8f).coerceIn(-1f, 1f)
            tiltY = (event.values[1] / 9.8f).coerceIn(-1f, 1f)
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
                        val dst = RectF((w-drawW)/2f, (h-drawH)/2f, (w+drawW)/2f, (h+drawH)/2f)
                        val depth = depthBitmap
                        if (depth != null && !depth.isRecycled) {
                            val vertices = FloatArray((meshColumns + 1) * (meshRows + 1) * 2)
                            var index = 0
                            for (row in 0..meshRows) {
                                val y = image.height * row.toFloat() / meshRows
                                for (col in 0..meshColumns) {
                                    val x = image.width * col.toFloat() / meshColumns
                                    val px = (x / image.width * (depth.width - 1)).toInt().coerceIn(0, depth.width - 1)
                                    val py = (y / image.height * (depth.height - 1)).toInt().coerceIn(0, depth.height - 1)
                                    val z = Color.red(depth.getPixel(px, py)) / 255f
                                    val nx = col.toFloat() / meshColumns - 0.5f
                                    val ny = row.toFloat() / meshRows - 0.5f
                                    val parallax = (z - 0.5f) * 2f
                                    vertices[index++] = dst.left + col.toFloat() / meshColumns * drawW + dx * parallax * 1.6f
                                    vertices[index++] = dst.top + row.toFloat() / meshRows * drawH + dy * parallax * 1.6f
                                }
                            }
                            canvas.drawBitmapMesh(image, meshColumns, meshRows, vertices, 0, null, 0, paint)
                        } else {
                            val shifted = RectF(dst.left + dx, dst.top + dy, dst.right + dx, dst.bottom + dy)
                            canvas.drawBitmap(image, null, shifted, paint)
                        }
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
                // Never leave the wallpaper surface black if mesh/depth rendering fails.
                try {
                    val safeCanvas = canvas
                    if (safeCanvas != null) {
                        safeCanvas.drawColor(Color.rgb(8, 12, 24))
                        val image = bitmap
                        if (image != null && !image.isRecycled) {
                            val scale = max(safeCanvas.width.toFloat() / image.width, safeCanvas.height.toFloat() / image.height)
                            val drawW = image.width * scale
                            val drawH = image.height * scale
                            val left = (safeCanvas.width - drawW) / 2f
                            val top = (safeCanvas.height - drawH) / 2f
                            safeCanvas.drawBitmap(image, null, RectF(left, top, left + drawW, top + drawH), paint)
                        } else {
                            paint.color = Color.WHITE
                            paint.textSize = 34f
                            safeCanvas.drawText("CLOCKSTOPPER", 28f, safeCanvas.height * 0.5f, paint)
                        }
                    }
                } catch (_: Exception) { }
            } finally {
                if (canvas != null) {
                    try { surfaceHolder.unlockCanvasAndPost(canvas) } catch (_: Exception) { }
                }
            }
        }

        override fun onDestroy() {
            visibleNow = false
            sensorManager.unregisterListener(this)
            bitmap?.recycle()
            depthBitmap?.recycle()
            bitmap = null
            depthBitmap = null
            super.onDestroy()
        }
    }
}
