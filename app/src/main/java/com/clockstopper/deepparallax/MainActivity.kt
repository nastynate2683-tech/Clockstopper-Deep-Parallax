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
    private val pickDepth = 42
    private val bg = Color.rgb(8,7,11)
    private val panel = Color.rgb(18,12,19)
    private val red = Color.rgb(255,35,74)
    private val softRed = Color.rgb(255,105,125)
    private val muted = Color.rgb(184,160,169)
    private val white = Color.rgb(250,240,244)
    private lateinit var imagePreview: ImageView
    private lateinit var previewLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll=ScrollView(this).apply { setBackgroundColor(bg);isFillViewport=true }
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER_HORIZONTAL;setPadding(dp(18),dp(20),dp(18),dp(24));background=shape(panel,Color.rgb(90,17,39),1,20) }
        fun label(text:String,size:Float,color:Int)=TextView(this).apply { this.text=text;textSize=size;setTextColor(color);gravity=Gravity.CENTER;setPadding(dp(6),dp(8),dp(6),dp(8)) }
        root.addView(label("CLOCKSTOPPER",27f,softRed).apply { typeface=Typeface.create("sans-serif-black",Typeface.BOLD);letterSpacing=.13f })
        root.addView(label("D E E P   P A R A L L A X",11f,muted))
        root.addView(label("BUILD YOUR WORLD IN DEPTH",13f,white).apply { typeface=Typeface.DEFAULT_BOLD })
        root.addView(label("Choose an image, paint near and far areas, then feel the depth.",14f,muted))
        root.addView(button("✦   CHOOSE WALLPAPER IMAGE") { launchPicker(pickImage) },buttonParams())
        previewLabel=label("FULL IMAGE PREVIEW  •  CHOOSE AN IMAGE",12f,softRed)
        root.addView(previewLabel)
        imagePreview=ImageView(this).apply {
            background=shape(Color.rgb(7,7,12),Color.rgb(115,24,48),1,14)
            scaleType=ImageView.ScaleType.FIT_CENTER
            adjustViewBounds=false
            setPadding(dp(4),dp(4),dp(4),dp(4))
        }
        root.addView(imagePreview,LinearLayout.LayoutParams(-1,dp(440)).apply { setMargins(0,dp(4),0,dp(8)) })
        restorePreview()
        root.addView(button("✎   PAINT DEPTH ON YOUR IMAGE") {
            if (prefs.getString("image_uri",null)==null) {
                Toast.makeText(this,"Choose a wallpaper image first.",Toast.LENGTH_SHORT).show()
            } else startActivity(Intent(this,DepthPaintActivity::class.java))
        },buttonParams())
        root.addView(button("◈   CHOOSE DEPTH MAP  ·  OPTIONAL") { launchPicker(pickDepth) },buttonParams())
        root.addView(button("CLEAR DEPTH MAP") {
            prefs.edit().remove("depth_uri").remove("depth_path").apply()
            Toast.makeText(this,"Depth map cleared",Toast.LENGTH_SHORT).show()
        },buttonParams(true))
        val motion=Switch(this).apply { text="  TILT MOVEMENT";textSize=15f;typeface=Typeface.DEFAULT_BOLD;setTextColor(white);buttonTintList=android.content.res.ColorStateList.valueOf(red);isChecked=prefs.getBoolean("motion_enabled",true);setPadding(dp(8),dp(12),dp(8),dp(8));setOnCheckedChangeListener { _,v->prefs.edit().putBoolean("motion_enabled",v).apply() } }
        root.addView(motion,matchWrap())
        root.addView(label("DEPTH PUSH  •  HOW FAR THE LAYERS SEPARATE",12f,softRed))
        root.addView(SeekBar(this).apply {
            max=100
            progress=prefs.getInt("strength",55)
            progressTintList=android.content.res.ColorStateList.valueOf(red)
            thumbTintList=android.content.res.ColorStateList.valueOf(softRed)
            setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s:SeekBar?,v:Int,from:Boolean) { prefs.edit().putInt("strength",v).apply() }
                override fun onStartTrackingTouch(s:SeekBar?) {}
                override fun onStopTrackingTouch(s:SeekBar?) {}
            })
        },matchWrap())
        root.addView(label("Higher values push near and far depth planes farther apart. Tilt to see the movement.",12f,muted))
        root.addView(button("▶   PREVIEW  /  SET LIVE WALLPAPER") { openWallpaperPreview() },buttonParams())
        scroll.addView(root);setContentView(scroll)
    }

    private fun restorePreview() {
        val saved=prefs.getString("image_uri",null) ?: return
        try {
            imagePreview.setImageURI(Uri.parse(saved))
            previewLabel.text="WALLPAPER PREVIEW  •  FULL IMAGE FIT"
        } catch (_:Exception) { previewLabel.text="IMAGE PREVIEW UNAVAILABLE  •  CHOOSE AGAIN" }
    }

    private fun openWallpaperPreview() {
        val component = ComponentName(this, ParallaxWallpaperService::class.java)
        val direct = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply { putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component) }
        try { startActivity(direct) }
        catch (_: ActivityNotFoundException) {
            try { startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));Toast.makeText(this,"Choose Clockstopper from the live wallpaper list.",Toast.LENGTH_LONG).show() }
            catch (_: Exception) { try { startActivity(Intent(Intent.ACTION_SET_WALLPAPER)) } catch (e: Exception) { Toast.makeText(this,"Android could not open its wallpaper screen: ${e.javaClass.simpleName}",Toast.LENGTH_LONG).show() } }
        } catch (e: Exception) { Toast.makeText(this,"Preview launch failed: ${e.javaClass.simpleName}",Toast.LENGTH_LONG).show() }
    }

    private fun launchPicker(request:Int) {
        val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE);type="image/*";addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION) }
        startActivityForResult(intent,request)
    }

    @Deprecated("Deprecated by Android, retained for compatibility")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(resultCode!=RESULT_OK || (requestCode!=pickImage && requestCode!=pickDepth)) return
        val uri=data?.data ?: return
        try { contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_:SecurityException) {}
        if(requestCode==pickImage) {
            prefs.edit().putString("image_uri",uri.toString()).remove("depth_path").remove("depth_uri").apply()
            imagePreview.setImageURI(uri);previewLabel.text="WALLPAPER PREVIEW  •  FULL IMAGE FIT"
        } else {
            prefs.edit().putString("depth_uri",uri.toString()).remove("depth_path").apply()
            Toast.makeText(this,"Depth map selected",Toast.LENGTH_SHORT).show()
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
