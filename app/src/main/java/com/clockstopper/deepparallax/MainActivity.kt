package com.clockstopper.deepparallax

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("parallax", MODE_PRIVATE) }
    private val pickImage = 41
    private val pickDepth = 42

    private val bg = Color.rgb(8, 7, 11)
    private val panel = Color.rgb(18, 12, 19)
    private val red = Color.rgb(255, 35, 74)
    private val softRed = Color.rgb(255, 105, 125)
    private val muted = Color.rgb(184, 160, 169)
    private val white = Color.rgb(250, 240, 244)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
        }
        val frame = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(2), dp(2), dp(2), dp(18))
            background = shape(Color.TRANSPARENT, red, 2, 22)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(26), dp(22), dp(24))
            background = shape(panel, Color.rgb(90, 17, 39), 1, 20)
        }

        val brand = TextView(this).apply {
            text = "CLOCKSTOPPER"
            textSize = 27f
            letterSpacing = 0.13f
            setTextColor(softRed)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        root.addView(brand, matchWrap())
        val subtitle = TextView(this).apply {
            text = "D E E P   P A R A L L A X"
            textSize = 11f
            letterSpacing = 0.18f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(18))
        }
        root.addView(subtitle, matchWrap())

        val intro = TextView(this).apply {
            text = "BUILD YOUR WORLD IN DEPTH"
            textSize = 13f
            letterSpacing = 0.08f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(14), dp(12), dp(8))
        }
        root.addView(intro, matchWrap())
        val description = TextView(this).apply {
            text = "Choose a wallpaper image and an optional grayscale depth map. White areas sit closer; black areas sit farther away."
            textSize = 14f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setLineSpacing(dp(3).toFloat(), 1f)
            setPadding(dp(4), 0, dp(4), dp(18))
        }
        root.addView(description, matchWrap())

        root.addView(neonButton("✦   CHOOSE WALLPAPER IMAGE") { launchPicker(pickImage) }, buttonParams())
        root.addView(neonButton("◈   CHOOSE DEPTH MAP  ·  OPTIONAL") { launchPicker(pickDepth) }, buttonParams())
        root.addView(neonButton("CLEAR DEPTH MAP") { prefs.edit().remove("depth_uri").apply() }, buttonParams(secondary = true))

        val motion = Switch(this).apply {
            text = "  TILT MOVEMENT"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(white)
            buttonTintList = android.content.res.ColorStateList.valueOf(red)
            isChecked = prefs.getBoolean("motion_enabled", true)
            setPadding(dp(8), dp(12), dp(8), dp(8))
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("motion_enabled", checked).apply()
            }
        }
        root.addView(motion, matchWrap())

        val strengthTitle = TextView(this).apply {
            text = "MOTION STRENGTH"
            textSize = 12f
            letterSpacing = 0.12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(softRed)
            setPadding(dp(8), dp(16), dp(8), dp(2))
        }
        root.addView(strengthTitle, matchWrap())
        root.addView(SeekBar(this).apply {
            max = 100
            progress = prefs.getInt("strength", 35)
            progressTintList = android.content.res.ColorStateList.valueOf(red)
            thumbTintList = android.content.res.ColorStateList.valueOf(softRed)
            setPadding(dp(8), 0, dp(8), dp(10))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, value: Int, fromUser: Boolean) {
                    prefs.edit().putInt("strength", value).apply()
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }, matchWrap())

        val note = TextView(this).apply {
            text = "The live wallpaper uses your phone's tilt sensor. No automatic drifting."
            textSize = 12f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(4), dp(8), dp(18))
        }
        root.addView(note, matchWrap())
        root.addView(neonButton("▶   PREVIEW  /  SET LIVE WALLPAPER") {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this@MainActivity, ParallaxWallpaperService::class.java))
            }
            startActivity(intent)
        }, buttonParams())

        val footer = TextView(this).apply {
            text = "CLOCKSTOPPER  •  MADE TO MOVE WITH YOU"
            textSize = 9f
            letterSpacing = 0.08f
            setTextColor(Color.rgb(119, 83, 96))
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, 0)
        }
        root.addView(footer, matchWrap())

        frame.addView(root, LinearLayout.LayoutParams(-1, -2))
        scroll.addView(frame)
        setContentView(scroll)
    }

    private fun neonButton(label: String, secondary: Boolean = false, action: () -> Unit) =
        Button(this).apply {
            text = label
            isAllCaps = false
            textSize = 13f
            letterSpacing = 0.04f
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setTextColor(if (secondary) softRed else white)
            background = shape(
                if (secondary) Color.rgb(25, 10, 18) else Color.rgb(67, 9, 27),
                if (secondary) Color.rgb(135, 27, 57) else red,
                if (secondary) 1 else 2,
                13
            )
            elevation = dp(5).toFloat()
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setOnClickListener { action() }
        }

    private fun shape(fill: Int, stroke: Int, strokeWidth: Int, radius: Int) =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(strokeWidth), stroke)
        }

    private fun matchWrap() = LinearLayout.LayoutParams(-1, -2)
    private fun buttonParams(secondary: Boolean = false) =
        LinearLayout.LayoutParams(-1, dp(if (secondary) 48 else 54)).apply {
            setMargins(0, dp(5), 0, dp(5))
        }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun launchPicker(request: Int) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, request)
    }

    @Deprecated("Deprecated by Android, retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if ((requestCode == pickImage || requestCode == pickDepth) && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { }
            val key = if (requestCode == pickDepth) "depth_uri" else "image_uri"
            prefs.edit().putString(key, uri.toString()).apply()
        }
    }
}
