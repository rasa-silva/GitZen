package com.zenhub.auth

import android.accounts.*
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder


class GitHubAuthenticator(private val ctx: Context) : AbstractAccountAuthenticator(ctx) {
    override fun getAuthTokenLabel(p0: String?): String? {
        return null
    }

    override fun confirmCredentials(p0: AccountAuthenticatorResponse?, p1: Account?, p2: Bundle?): Bundle? {
        return null
    }

    override fun updateCredentials(p0: AccountAuthenticatorResponse?, p1: Account?, p2: String?, p3: Bundle?): Bundle? {
        return null
    }

    override fun getAuthToken(response: AccountAuthenticatorResponse, account: Account, authTokenType: String, options: Bundle?): Bundle? {
        val am = AccountManager.get(ctx)
        val authToken = am.peekAuthToken(account, authTokenType)
        val result = Bundle()
        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
        result.putString(AccountManager.KEY_AUTHTOKEN, authToken)
        return result
    }

    override fun hasFeatures(p0: AccountAuthenticatorResponse?, p1: Account?, p2: Array<out String>?): Bundle? {
        return null
    }

    override fun editProperties(p0: AccountAuthenticatorResponse?, p1: String?): Bundle? {
        return null
    }

    @Throws(NetworkErrorException::class)
    override fun addAccount(response: AccountAuthenticatorResponse,
                            accountType: String, authTokenType: String?,
                            requiredFeatures: Array<String>?, options: Bundle?): Bundle? {
        val intent = Intent(ctx, LoginActivity::class.java)
        val bundle = Bundle()
        bundle.putParcelable(AccountManager.KEY_INTENT, intent)
        return bundle
    }
}

class AuthenticatorService : Service() {

    private val auth = GitHubAuthenticator(this)

    override fun onBind(intent: Intent): IBinder {
        return auth.iBinder
    }

}