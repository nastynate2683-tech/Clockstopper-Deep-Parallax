package com.clockstopper.deepparallax

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.io.File

class DepthPaintActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("parallax", MODE_PRIVATE) }
    private lateinit var paintView: DepthPaintView
    private val red = Color.rgb(255,35,74)
    private val white = Color.rgb(250,240,244)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(8,7,11))
            setPadding(dp(10),dp(8),dp(10),dp(10))
        }
        val title = TextView(this).apply {
            text = "PAINT DEPTH"
            textSize = 20f
            setTextColor(white)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0,dp(5),0,dp(5))
        }
        root.addView(title,LinearLayout.LayoutParams(-1,-2))
        val imageUri = prefs.getString("image_uri",null)
        if (imageUri == null) { finish();return }
        paintView = DepthPaintView(this)
        try { paintView.load(Uri.parse(imageUri)) } catch (_:Exception) {}
        root.addView(paintView,LinearLayout.LayoutParams(-1,0,1f).apply {
            setMargins(0,dp(4),0,dp(6))
        })
        val legend = TextView(this).apply {
            text = "WHITE = NEAR     BLACK = FAR\nGRAY = UNPAINTED"
            textSize = 11f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0,dp(3),0,dp(3))
        }
        root.addView(legend,LinearLayout.LayoutParams(-1,-2))
        val modes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        val near = Button(this).apply { text = "Paint NEAR"; isAllCaps = false; setOnClickListener { paintView.setForeground(true) } }
        val far = Button(this).apply { text = "Paint FAR"; isAllCaps = false; setOnClickListener { paintView.setForeground(false) } }
        modes.addView(near,LinearLayout.LayoutParams(0,dp(48),1f).apply { setMargins(dp(3),dp(2),dp(3),dp(2)) })
        modes.addView(far,LinearLayout.LayoutParams(0,dp(48),1f).apply { setMargins(dp(3),dp(2),dp(3),dp(2)) })
        root.addView(modes)
        val brushLabel = TextView(this).apply { text = "BRUSH SIZE";setTextColor(white);textSize=12f;gravity=Gravity.CENTER }
        root.addView(brushLabel)
        root.addView(SeekBar(this).apply {
            max = 142
            progress = 34
            progressTintList = android.content.res.ColorStateList.valueOf(red)
            thumbTintList = android.content.res.ColorStateList.valueOf(red)
            setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(bar:SeekBar?,value:Int,fromUser:Boolean) { paintView.setBrush((value+8).toFloat()) }
                override fun onStartTrackingTouch(bar:SeekBar?) {}
                override fun onStopTrackingTouch(bar:SeekBar?) {}
            })
        },LinearLayout.LayoutParams(-1,-2))
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val cancel = Button(this).apply { text="CANCEL";isAllCaps=false;setOnClickListener { finish() } }
        val save = Button(this).apply { text="SAVE DEPTH";isAllCaps=false;setOnClickListener {
            val file = File(filesDir,"clockstopper-depth.png")
            if (paintView.save(file.absolutePath)) {
                prefs.edit().putString("depth_path",file.absolutePath).remove("depth_uri").apply()
                Toast.makeText(this@DepthPaintActivity,"Depth map saved and applied.",Toast.LENGTH_LONG).show()
                finish()
            } else Toast.makeText(this@DepthPaintActivity,"Could not save the depth map.",Toast.LENGTH_LONG).show()
        } }
        actions.addView(cancel,LinearLayout.LayoutParams(0,dp(50),1f).apply { setMargins(dp(3),dp(5),dp(3),0) })
        actions.addView(save,LinearLayout.LayoutParams(0,dp(50),1f).apply { setMargins(dp(3),dp(5),dp(3),0) })
        root.addView(actions)
        setContentView(root)
    }

    private fun dp(value:Int) = (value*resources.displayMetrics.density).toInt()
}
