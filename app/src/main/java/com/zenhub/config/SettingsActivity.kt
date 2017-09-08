package com.zenhub.config

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceFragment
import android.support.v7.app.AppCompatActivity
import com.zenhub.LauncherActivity
import com.zenhub.R

class SettingsActivity : AppCompatActivity() {

    private val themeChanged by lazy { intent.getBooleanExtra("THEME_CHANGED", false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, SettingsFragment())
                .commit()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (themeChanged) {
            val intent = Intent(this, LauncherActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }

    companion object {
        const val KEY_PREF_DARK_THEME = "pref_dark_theme"
    }

    class SettingsFragment : PreferenceFragment() {

        private val changeListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                SettingsActivity.KEY_PREF_DARK_THEME -> {
                    switchTheme(prefs.getBoolean(key, true))
                    val intent = Intent(this.activity, SettingsActivity::class.java)
                    intent.putExtra("THEME_CHANGED", true)
                    startActivity(intent)
                    activity.finish()
                }
            }
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.preferences)
        }

        override fun onResume() {
            super.onResume()
            preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(changeListener)
        }

        override fun onPause() {
            super.onPause()
            preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(changeListener)
        }
    }
}

