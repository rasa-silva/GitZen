package com.zenhub.config

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.method.LinkMovementMethod
import com.zenhub.BuildConfig
import com.zenhub.R
import kotlinx.android.synthetic.main.activity_about.*


class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        app_version_value.text = BuildConfig.VERSION_NAME

        rate_button.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("market://details?id=${BuildConfig.APPLICATION_ID}")
            startActivity(intent)
        }

        support_lib.movementMethod = LinkMovementMethod.getInstance()
        kotlin.movementMethod = LinkMovementMethod.getInstance()
        kotlin_coroutines.movementMethod = LinkMovementMethod.getInstance()
        retrofit.movementMethod = LinkMovementMethod.getInstance()
        picasso.movementMethod = LinkMovementMethod.getInstance()
        highlightjs.movementMethod = LinkMovementMethod.getInstance()
        kotlin_coroutines_retrofit.movementMethod = LinkMovementMethod.getInstance()

    }
}
