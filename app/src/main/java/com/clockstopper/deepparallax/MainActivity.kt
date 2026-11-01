package com.clockstopper.deepparallax

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import android.text.TextUtils

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("parallax", MODE_PRIVATE) }
    private val pickImage = 41
    private val bg = Color.rgb(8,7,11)
    private val panel = Color.rgb(18,12,19)
    private val red = Color.rgb(255,35,74)
    private val softRed = Color.rgb(255,105,125)
    private val muted = Color.rgb(184,160,169)
    private val white = Color.rgb(250,240,244)
    private lateinit var editor: DepthPaintView
    private lateinit var depthLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll=ScrollView(this).apply { setBackgroundColor(bg);isFillViewport=true }
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(22),dp(24),dp(22),dp(24));background=shape(panel,Color.rgb(90,17,39),1,20) }
        fun label(text:String,size:Float,color:Int)=TextView(this).apply { this.text=text;textSize=size;setTextColor(color);gravity=Gravity.CENTER;setPadding(dp(6),dp(8),dp(6),dp(8)) }
        root.addView(label("CLOCKSTOPPER",27f,softRed).apply { typeface=Typeface.create("sans-serif-black",Typeface.BOLD);letterSpacing=.13f })
        root.addView(label("D E E P   P A R A L L A X",11f,muted))
        root.addView(label("BUILD YOUR WORLD IN DEPTH",13f,white).apply { typeface=Typeface.DEFAULT_BOLD })
        root.addView(label("Choose one wallpaper, then paint its depth with your finger. White is near; black is far.",14f,muted))
        root.addView(button("✦   CHOOSE WALLPAPER IMAGE") { launchPicker() },buttonParams())
        editor=DepthPaintView(this).apply { setBackgroundColor(Color.rgb(15,12,18)) }
        root.addView(editor,LinearLayout.LayoutParams(-1,dp(300)).apply { setMargins(0,dp(10),0,dp(8)) })
        depthLabel=label("DEPTH EDITOR  •  SELECT AN IMAGE FIRST",12f,softRed)
        root.addView(depthLabel)
        val tools=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER }
        tools.addView(button("FOREGROUND") { editor.setForeground(true);depthLabel.text="PAINTING FOREGROUND  •  WHITE = NEAR" },LinearLayout.LayoutParams(0,dp(48),1f).apply { setMargins(dp(3),0,dp(3),0) })
        tools.addView(button("BACKGROUND") { editor.setForeground(false);depthLabel.text="PAINTING BACKGROUND  •  BLACK = FAR" },LinearLayout.LayoutParams(0,dp(48),1f).apply { setMargins(dp(3),0,dp(3),0) })
        root.addView(tools)
        root.addView(label("BRUSH SIZE",12f,softRed))
        root.addView(SeekBar(this).apply { max=142;progress=34;progressTintList=android.content.res.ColorStateList.valueOf(red);thumbTintList=android.content.res.ColorStateList.valueOf(softRed);setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s:SeekBar?,v:Int,from:Boolean) { editor.setBrush((v+8).toFloat()) }
            override fun onStartTrackingTouch(s:SeekBar?) {}
            override fun onStopTrackingTouch(s:SeekBar?) {}
        }) },matchWrap())
        root.addView(button("SAVE DEPTH PAINTING") {
            val path=java.io.File(filesDir,"clockstopper-depth.png").absolutePath
            if(editor.hasImage() && editor.save(path)) { prefs.edit().putString("depth_path",path).remove("depth_uri").apply();depthLabel.text="DEPTH SAVED  •  READY TO PREVIEW";Toast.makeText(this,"Depth layout saved",Toast.LENGTH_SHORT).show() }
            else Toast.makeText(this,"Choose a wallpaper image first",Toast.LENGTH_SHORT).show()
        },buttonParams())
        root.addView(button("CLEAR DEPTH PAINTING") { prefs.edit().remove("depth_path").apply();depthLabel.text="DEPTH CLEARED" },buttonParams(true))
        val motion=Switch(this).apply { text="  TILT MOVEMENT";textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(white);buttonTintList=android.content.res.ColorStateList.valueOf(red);isChecked=prefs.getBoolean("motion_enabled",true);setPadding(dp(8),dp(12),dp(8),dp(8));setOnCheckedChangeListener { _,v->prefs.edit().putBoolean("motion_enabled",v).apply() } }
        root.addView(motion,matchWrap())
        root.addView(label("PARALLAX STRENGTH",12f,softRed))
        root.addView(SeekBar(this).apply { max=100;progress=prefs.getInt("strength",55);progressTintList=android.content.res.ColorStateList.valueOf(red);thumbTintList=android.content.res.ColorStateList.valueOf(softRed);setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s:SeekBar?,v:Int,from:Boolean) { prefs.edit().putInt("strength",v).apply() }
            override fun onStartTrackingTouch(s:SeekBar?) {}
            override fun onStopTrackingTouch(s:SeekBar?) {}
        }) },matchWrap())
        root.addView(label("Tilt your phone to move the layers. No automatic drifting.",12f,muted))
        root.addView(button("▶   PREVIEW  /  SET LIVE WALLPAPER") {
            openWallpaperPreview()
        },buttonParams())
        scroll.addView(root);setContentView(scroll)
    }
    private fun openWallpaperPreview() {
        val component = ComponentName(this, ParallaxWallpaperService::class.java)
        val direct = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
        }
        try {
            startActivity(direct)
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
                Toast.makeText(this, "Choose Clockstopper from the live wallpaper list.", Toast.LENGTH_LONG).show()
            } catch (_: Exception) {
                try {
                    startActivity(Intent(WallpaperManager.ACTION_SET_WALLPAPER))
                } catch (e: Exception) {
                    Toast.makeText(this, "Android could not open its wallpaper screen: ${e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Preview launch failed: ${e.javaClass.simpleName}", Toast.LENGTH_LONG).show()
        }
    }

    private fun launchPicker() {
        val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE);type="image/*";addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) }
        startActivityForResult(intent,pickImage)
    }
    @Deprecated("Deprecated by Android, retained for compatibility")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==pickImage && resultCode==RESULT_OK) {
            val uri=data?.data ?: return
            try { contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_:SecurityException) {}
            prefs.edit().putString("image_uri",uri.toString()).remove("depth_path").apply()
            try { editor.load(uri);depthLabel.text="PAINT WHITE FOREGROUND OR BLACK BACKGROUND" } catch (_:Exception) { Toast.makeText(this,"Could not open that image",Toast.LENGTH_LONG).show() }
        }
    }
    private fun button(text:String,action:()->Unit)=Button(this).apply {
        this.text=text;isAllCaps=false;textSize=11f;maxLines=1;ellipsize=TextUtils.TruncateAt.END;includeFontPadding=false;letterSpacing=.01f;typeface=Typeface.DEFAULT_BOLD;setTextColor(white);background=shape(Color.rgb(67,9,27),red,2,13);setPadding(dp(6),dp(8),dp(6),dp(8));setOnClickListener { action() }
    }
    private fun shape(fill:Int,stroke:Int,width:Int,radius:Int)=GradientDrawable().apply { setColor(fill);cornerRadius=dp(radius).toFloat();setStroke(dp(width),stroke) }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun matchWrap()=LinearLayout.LayoutParams(-1,-2)
    private fun buttonParams(secondary:Boolean=false)=LinearLayout.LayoutParams(-1,dp(if(secondary)48 else 54)).apply { setMargins(0,dp(5),0,dp(5)) }
}
