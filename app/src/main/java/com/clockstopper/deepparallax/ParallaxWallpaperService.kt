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
            if (visibleNow) {
                loadImages()
                drawFrame()
            }
        }

        private fun decode(uriText: String?): Bitmap? {
            if (uriText.isNullOrBlank()) return null
            return try {
                when {
                    uriText.startsWith("/") -> BitmapFactory.decodeFile(uriText)
                    uriText.startsWith("file://") -> BitmapFactory.decodeFile(Uri.parse(uriText).path)
                    else -> contentResolver.openInputStream(Uri.parse(uriText))?.use { BitmapFactory.decodeStream(it) }
                }
            } catch (_: Exception) {
                null
            }
        }

        private fun loadImages() {
            val imageUri = prefs.getString("image_uri", null)
            if (imageUri != loadedUri || bitmap == null || bitmap?.isRecycled == true) {
                bitmap?.recycle()
                bitmap = decode(imageUri)
                loadedUri = if (bitmap != null) imageUri else null
            }

            val depthPath = prefs.getString("depth_path", null)
            if (depthPath != loadedDepthUri || (depthPath != null && depthBitmap == null)) {
                depthBitmap?.recycle()
                depthBitmap = decode(depthPath)
                loadedDepthUri = if (depthBitmap != null) depthPath else null
            }
        }

        private fun buildDepthMesh(
            image: Bitmap,
            depth: Bitmap,
            dst: RectF,
            dx: Float,
            dy: Float,
            strength: Float
        ): FloatArray {
            val vertices = FloatArray((meshColumns + 1) * (meshRows + 1) * 2)
            var out = 0
            val depthScale = 5.0f * strength
            for (row in 0..meshRows) {
                val v = row.toFloat() / meshRows
                val sy = (v * (depth.height - 1)).toInt().coerceIn(0, depth.height - 1)
                for (col in 0..meshColumns) {
                    val u = col.toFloat() / meshColumns
                    val sx = (u * (depth.width - 1)).toInt().coerceIn(0, depth.width - 1)
                    val pixel = depth.getPixel(sx, sy)
                    val gray = (Color.red(pixel) * 0.299f + Color.green(pixel) * 0.587f + Color.blue(pixel) * 0.114f) / 255f
                    val offset = (gray - 0.5f) * 2f
                    vertices[out++] = image.width * u + (dx / max(1f, dst.width()) * image.width) * offset * depthScale
                    vertices[out++] = image.height * v + (dy / max(1f, dst.height()) * image.height) * offset * depthScale
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

        private fun drawPlain(canvas: Canvas, image: Bitmap, rect: RectF) {
            if (!image.isRecycled) canvas.drawBitmap(image, null, rect, paint)
        }

        private fun drawFrame() {
            if (!visibleNow) return
            val currentImageUri = prefs.getString("image_uri", null)
            val currentDepthPath = prefs.getString("depth_path", null)
            if (bitmap == null || loadedUri != currentImageUri || loadedDepthUri != currentDepthPath) loadImages()

            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas == null) return
                val w = canvas.width.toFloat()
                val h = canvas.height.toFloat()
                canvas.drawColor(Color.rgb(8, 12, 24))
                val image = bitmap
                if (image == null || image.isRecycled) {
                    paint.color = Color.rgb(226, 48, 72)
                    paint.style = Paint.Style.FILL
                    paint.textSize = 42f
                    canvas.drawText("CLOCKSTOPPER", 32f, h * 0.48f, paint)
                    paint.color = Color.LTGRAY
                    paint.textSize = 22f
                    canvas.drawText("CHOOSE AN IMAGE TO BEGIN", 34f, h * 0.48f + 38f, paint)
                    return
                }

                val scale = max(w / image.width, h / image.height)
                val drawW = image.width * scale
                val drawH = image.height * scale
                val strength = if (prefs.getBoolean("motion_enabled", true)) prefs.getInt("strength", 55).coerceIn(0, 100) / 100f else 0f
                val range = min(w, h) * 0.16f * strength
                val dx = smoothX * range
                val dy = -smoothY * range
                val dst = RectF((w - drawW) / 2f, (h - drawH) / 2f, (w + drawW) / 2f, (h + drawH) / 2f)
                val shifted = RectF(dst.left + dx, dst.top + dy, dst.right + dx, dst.bottom + dy)

                // Always draw a normal image first. That guarantees the preview cannot
                // become a black screen just because a depth mesh fails or is malformed.
                drawPlain(canvas, image, shifted)

                val depth = depthBitmap
                if (depth != null && !depth.isRecycled && strength > 0f) {
                    try {
                        val vertices = buildDepthMesh(image, depth, shifted, dx, dy, strength)
                        canvas.save()
                        canvas.translate(shifted.left, shifted.top)
                        canvas.scale(scale, scale)
                        canvas.drawBitmapMesh(image, meshColumns, meshRows, vertices, 0, null, 0, paint)
                        canvas.restore()
                    } catch (_: Throwable) {
                        // The normal image is already on the surface; keep it.
                    }
                }

                paint.color = 0x35FFFFFF
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                val inset = min(w, h) * 0.08f
                canvas.drawRoundRect(RectF(inset + dx * 1.35f, inset + dy * 1.35f, w - inset + dx * 1.35f, h - inset + dy * 1.35f), 28f, 28f, paint)
                paint.style = Paint.Style.FILL
                paint.color = Color.WHITE
            } catch (_: Throwable) {
                // If anything unexpected happens, make one final attempt to show the
                // normal wallpaper instead of leaving a black surface.
                try {
                    if (canvas != null) {
                        canvas.drawColor(Color.rgb(8, 12, 24))
                        bitmap?.let { if (!it.isRecycled) {
                            val scale = max(canvas.width.toFloat() / it.width, canvas.height.toFloat() / it.height)
                            val drawW = it.width * scale
                            val drawH = it.height * scale
                            drawPlain(canvas, it, RectF((canvas.width - drawW) / 2f, (canvas.height - drawH) / 2f, (canvas.width + drawW) / 2f, (canvas.height + drawH) / 2f))
                        }}
                    }
                } catch (_: Throwable) { }
            } finally {
                if (canvas != null) {
                    try { surfaceHolder.unlockCanvasAndPost(canvas) } catch (_: Throwable) { }
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
