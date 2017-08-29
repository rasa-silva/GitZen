package com.zenhub

import android.content.Intent
import android.os.Bundle
import com.zenhub.auth.LoggedUser
import com.zenhub.auth.LoginActivity
import com.zenhub.core.BaseActivity
import com.zenhub.user.UserDetailsActivity

class LauncherActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (LoggedUser.account == null) {
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            startActivity(Intent(this, UserDetailsActivity::class.java))
        }
        finish()
    }
}
