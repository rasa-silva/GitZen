package com.zenhub.auth

import android.accounts.Account
import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.preference.PreferenceManager
import com.zenhub.Application

object LoggedUser {

    private val ACCOUNT_TYPE = "GitHub"
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(Application.context)
    var account: Account? = null

    init {
        val am = AccountManager.get(Application.context)
        val accountsByType = am.getAccountsByType(ACCOUNT_TYPE)
        if (accountsByType.isNotEmpty()) account = accountsByType[0]
    }

    fun setAccount(username: String, token: String) {
        sharedPreferences.edit().putString("username", username).apply()
        val am = AccountManager.get(Application.context)
        account = Account(username, ACCOUNT_TYPE)
        am.addAccountExplicitly(account, null, null)
        am.setAuthToken(account, "GitHub", token)
    }

    @SuppressLint("MissingPermission")
    fun getToken(): String? {
        val am = AccountManager.get(Application.context)
        return am.blockingGetAuthToken(account, "GitHub", true)
    }
}
