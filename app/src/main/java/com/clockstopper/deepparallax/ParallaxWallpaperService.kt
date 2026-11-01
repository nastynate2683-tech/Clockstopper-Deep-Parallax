package com.clockstopper.deepparallax

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color

/**
 * Safe first-stage wallpaper shell. It draws only while visible and does not
 * register global listeners, alter system settings, or animate automatically.
 * Layered GPU/depth-map rendering is the next milestone.
 */
class ParallaxWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = ParallaxEngine()

    inner class ParallaxEngine : Engine() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private var visibleNow = false

        override fun onVisibilityChanged(visible: Boolean) {
            visibleNow = visible
            if (visibleNow) drawFrame()
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            if (visibleNow) drawFrame()
        }

        private fun drawFrame() {
            if (!visibleNow) return
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    canvas.drawColor(Color.rgb(12, 17, 31))
                    paint.color = Color.rgb(220, 45, 65)
                    paint.textSize = 42f
                    canvas.drawText("CLOCKSTOPPER", 36f, canvas.height * 0.48f, paint)
                    paint.color = Color.LTGRAY
                    paint.textSize = 22f
                    canvas.drawText("DEEP PARALLAX", 38f, canvas.height * 0.48f + 38f, paint)
                }
            } finally {
                if (canvas != null) surfaceHolder.unlockCanvasAndPost(canvas)
            }
        }

        override fun onDestroy() {
            visibleNow = false
            super.onDestroy()
        }
    }
}
