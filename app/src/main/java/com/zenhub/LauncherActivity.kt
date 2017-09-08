package com.zenhub

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import com.zenhub.auth.LoggedUser
import com.zenhub.auth.LoginActivity
import com.zenhub.config.SettingsActivity
import com.zenhub.config.switchTheme
import com.zenhub.core.BaseActivity
import com.zenhub.user.UserDetailsActivity

class LauncherActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val useDarkTheme = prefs.getBoolean(SettingsActivity.KEY_PREF_DARK_THEME, true)
        switchTheme(useDarkTheme)

        if (LoggedUser.account == null) {
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            startActivity(Intent(this, UserDetailsActivity::class.java))
        }
        finish()
    }
}
