package com.zenhub

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import com.zenhub.github.ErrorMessage
import com.zenhub.github.GSON

@SuppressLint("StaticFieldLeak")
class Application : android.app.Application() {

//    init {
//        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
//    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        picasso = Picasso.with(context)
//        picasso.setIndicatorsEnabled(true)
    }

    companion object {

        lateinit var context: Context
        lateinit var picasso: Picasso
        const val LOGTAG = "ZenHub"
    }
}

class RoundedTransformation(private val radius: Float? = null, private val margin: Float = 0f) : Transformation {

    private val key = "rounded(radius=$radius, margin=$margin)"

    override fun transform(source: Bitmap): Bitmap {
        val paint = Paint().apply {
            isAntiAlias = true
            shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(output).drawRoundRect(margin, margin, source.width - margin, source.height - margin,
                radius ?: source.width.toFloat() / 2, radius ?: source.height.toFloat() / 2,
                paint)
        if (source != output) {
            source.recycle()
        }
        return output
    }

    override fun key() = key
}


fun showErrorOnSnackbar(rootView: View, error: String) {
    Log.e(Application.LOGTAG, "Failed response: $error")
    val errorMessage = GSON.fromJson<ErrorMessage>(error, ErrorMessage::class.java)
    val snackbar = Snackbar.make(rootView, errorMessage.message, Snackbar.LENGTH_INDEFINITE)
    snackbar.view.setBackgroundColor(0xfffb4934.toInt())
    snackbar.show()
}