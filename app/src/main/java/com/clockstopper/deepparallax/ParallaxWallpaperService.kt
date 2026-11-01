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
        private val meshColumns = 64
        private val meshRows = 96

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
            if (uriText.isNullOrBlank()) return null
            return try {
                if (uriText.startsWith("/")) BitmapFactory.decodeFile(uriText)
                else contentResolver.openInputStream(Uri.parse(uriText))?.use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) { null }
        }

        // Reload on every wallpaper reveal. If Android temporarily denies a document
        // URI, do not cache the failed result; a later reveal can retry it.
        private fun loadImages() {
            val imageUri = prefs.getString("image_uri", null)
            if (imageUri != loadedUri || bitmap == null || bitmap?.isRecycled == true) {
                bitmap?.recycle()
                bitmap = decode(imageUri)
                loadedUri = if (bitmap != null) imageUri else null
            }

            val depthUri = prefs.getString("depth_path", null)
            if (depthUri != loadedDepthUri || (depthUri != null && depthBitmap == null)) {
                depthBitmap?.recycle()
                depthBitmap = decode(depthUri)
                loadedDepthUri = if (depthBitmap != null) depthUri else null
            }
        }


        private fun buildDepthMesh(
            image: Bitmap,
            depth: Bitmap,
            dst: RectF,
            dx: Float,
            dy: Float
        ): FloatArray {
            val vertices = FloatArray((meshColumns + 1) * (meshRows + 1) * 2)
            var out = 0
            for (row in 0..meshRows) {
                val v = row.toFloat() / meshRows
                val sy = (v * (depth.height - 1)).toInt().coerceIn(0, depth.height - 1)
                for (col in 0..meshColumns) {
                    val u = col.toFloat() / meshColumns
                    val sx = (u * (depth.width - 1)).toInt().coerceIn(0, depth.width - 1)
                    val pixel = depth.getPixel(sx, sy)
                    val gray = (Color.red(pixel) * 0.299f + Color.green(pixel) * 0.587f + Color.blue(pixel) * 0.114f) / 255f
                    val offset = (gray - 0.5f) * 2f
                    // BitmapMesh vertices are in bitmap coordinates, not screen coordinates.
                    vertices[out++] = image.width * u + (dx / dst.width() * image.width) * offset * 3.2f
                    vertices[out++] = image.height * v + (dy / dst.height() * image.height) * offset * 3.2f
                }
            }
            return vertices
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
            if (bitmap == null || loadedUri != prefs.getString("image_uri", null) ||
                loadedDepthUri != prefs.getString("depth_path", null)) loadImages()
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
                        val strength = if (prefs.getBoolean("motion_enabled", true)) prefs.getInt("strength", 55) / 100f else 0f
                        val range = min(w, h) * 0.12f * strength
                        val dx = smoothX * range
                        val dy = -smoothY * range
                        val dst = RectF((w-drawW)/2f, (h-drawH)/2f, (w+drawW)/2f, (h+drawH)/2f)
                        val shifted = RectF(dst.left + dx, dst.top + dy, dst.right + dx, dst.bottom + dy)
                        val depth = depthBitmap
                        if (depth != null && !depth.isRecycled) {
                            try {
                                val vertices = buildDepthMesh(image, depth, shifted, dx, dy)
                                canvas.save()
                                canvas.translate(shifted.left, shifted.top)
                                canvas.scale(scale, scale)
                                canvas.drawBitmapMesh(image, meshColumns, meshRows, vertices, 0, null, 0, paint)
                                canvas.restore()
                            } catch (_: Exception) {
                                canvas.drawBitmap(image, null, shifted, paint)
                            }
                        } else {
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
