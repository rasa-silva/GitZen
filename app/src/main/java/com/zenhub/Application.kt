package com.zenhub

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.support.design.widget.NavigationView
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.view.View
import com.google.gson.Gson

import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation
import com.zenhub.github.ErrorMessage
import com.zenhub.user.OwnReposActivity
import com.zenhub.user.StarredReposActivity
import com.zenhub.user.UserDetailsActivity

@SuppressLint("StaticFieldLeak")
class Application : android.app.Application() {

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        picasso = Picasso.with(context)
//        picasso.setIndicatorsEnabled(true)
    }

    companion object {

        lateinit var context: Context
        lateinit var picasso: Picasso
        var LOGTAG = "ZenHub"
        val GSON = Gson()
    }
}

open class BaseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout

    protected fun onCreateDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val drawerToggle = ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0)
        drawerLayout.addDrawerListener(drawerToggle)

        drawerToggle.syncState()

        findViewById<NavigationView>(R.id.nav_view)?.setNavigationItemSelectedListener(this)

        findViewById<SwipeRefreshLayout>(R.id.swiperefresh)?.setOnRefreshListener {
            requestDataRefresh()
        }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_home -> startActivity(Intent(this, UserDetailsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            R.id.nav_repos -> startActivity(Intent(this, OwnReposActivity::class.java))
            R.id.nav_starred -> startActivity(Intent(this, StarredReposActivity::class.java))
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    protected open fun requestDataRefresh() {}
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
    val errorMessage = Application.GSON.fromJson<ErrorMessage>(error, ErrorMessage::class.java)
    val snackbar = Snackbar.make(rootView, errorMessage.message, Snackbar.LENGTH_INDEFINITE)
    snackbar.view.setBackgroundColor(0xfffb4934.toInt())
    snackbar.show()
}