package com.clockstopper.deepparallax

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("parallax", MODE_PRIVATE) }
    private val pickImage = 41

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 36, 28, 24)
        }
        root.addView(TextView(this).apply {
            text = "CLOCKSTOPPER\nDEEP PARALLAX"
            textSize = 26f
            gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = "Choose a photo, then set the live wallpaper. Tilt movement is local to this wallpaper; nothing changes across the rest of your phone."
            textSize = 16f
            setPadding(0, 22, 0, 18)
        })
        root.addView(Button(this).apply {
            text = "Choose wallpaper image"
            setOnClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                }
                startActivityForResult(intent, pickImage)
            }
        })
        val motion = Switch(this).apply {
            text = "Tilt movement enabled"
            isChecked = prefs.getBoolean("motion_enabled", true)
            setOnCheckedChangeListener { _, checked -> prefs.edit().putBoolean("motion_enabled", checked).apply() }
        }
        root.addView(motion)
        root.addView(TextView(this).apply { text = "Tilt strength"; textSize = 16f; setPadding(0, 20, 0, 4) })
        val strength = SeekBar(this).apply {
            max = 100
            progress = prefs.getInt("strength", 35)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                    prefs.edit().putInt("strength", value).apply()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        root.addView(strength)
        root.addView(Button(this).apply {
            text = "Preview / Set live wallpaper"
            setOnClickListener {
                val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                    putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                        ComponentName(this@MainActivity, ParallaxWallpaperService::class.java))
                }
                startActivity(intent)
            }
        })
        setContentView(root)
    }

    @Deprecated("Deprecated by Android, retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickImage && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                prefs.edit().putString("image_uri", uri.toString()).apply()
            } catch (_: SecurityException) {
                prefs.edit().putString("image_uri", uri.toString()).apply()
            }
        }
    }
}
