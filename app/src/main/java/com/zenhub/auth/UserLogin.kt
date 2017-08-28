package com.zenhub.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.preference.PreferenceManager
import com.zenhub.Application

object UserLogin {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Application.context)

    fun clearPreferences() = sharedPreferences.edit().clear().apply()

    fun getUser(): String? {
        return sharedPreferences.getString("username", null)
    }

    fun addAccount(username: String, token: String) {
        sharedPreferences.edit().putString("username", username).apply()
        val am = AccountManager.get(Application.context)
        val account = Account(username, "GitHub")
        am.addAccountExplicitly(account, null, null)
        am.setAuthToken(account, "GitHub", token)
    }

    @SuppressLint("MissingPermission")
    fun getToken(): String? {
        val am = AccountManager.get(Application.context)
        val account = Account(getUser(), "GitHub")
        val authToken = am.blockingGetAuthToken(account, "GitHub", true)
        return authToken
    }
}
