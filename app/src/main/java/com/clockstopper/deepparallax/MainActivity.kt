package com.clockstopper.deepparallax

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
        }
        layout.addView(TextView(this).apply {
            text = "CLOCKSTOPPER\nDEEP PARALLAX\n\nA clean, tilt-driven wallpaper engine.\nNo automatic drift. Motion stays inside this wallpaper."
            textSize = 20f
        })
        layout.addView(Button(this).apply {
            text = "Preview / Set wallpaper"
            setOnClickListener {
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(this@MainActivity, ParallaxWallpaperService::class.java))
                }
                startActivity(intent)
            }
        })
        setContentView(layout)
    }
}
