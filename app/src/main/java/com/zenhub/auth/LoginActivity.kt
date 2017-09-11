package com.zenhub.auth

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.zenhub.*
import com.zenhub.core.BaseActivity
import com.zenhub.github.gitHubService
import com.zenhub.github.mappings.TokenRequest
import com.zenhub.user.UserDetailsActivity
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import okhttp3.Credentials
import ru.gildor.coroutines.retrofit.Result
import ru.gildor.coroutines.retrofit.awaitResult


class LoginActivity : BaseActivity() {

    private var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null
    private var mResultBundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        accountAuthenticatorResponse = intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
        accountAuthenticatorResponse?.onRequestContinued()

        setContentView(R.layout.activity_login)
        sign_in_button.setOnClickListener { attemptLogin() }
    }

    override fun finish() {
        accountAuthenticatorResponse?.let {
            // send the result bundle back if set, otherwise send an error.
            if (mResultBundle != null) {
                accountAuthenticatorResponse?.onResult(mResultBundle)
            } else {
                accountAuthenticatorResponse?.onError(AccountManager.ERROR_CODE_CANCELED, "canceled")
            }
            accountAuthenticatorResponse = null
        }

        super.finish()
    }

    private fun setAccountAuthenticatorResult(result: Bundle) {
        mResultBundle = result
    }

    private fun attemptLogin() {
        showProgress(true)

        val user = username.text.toString()
        val pwd = password.text.toString()

        launch(UI) {
            val scopes = listOf("gist", "repo", "user")
            val request = TokenRequest(BuildConfig.GITHUB_CLIENT_ID, BuildConfig.GITHUB_SECRET, scopes, "ZenHub")
            val basic = Credentials.basic(user, pwd)
            val tokenResult = gitHubService.createToken(basic, request).awaitResult()
            when (tokenResult) {
                is Result.Ok -> {
                    LoggedUser.setAccount(user, tokenResult.value.token)
                    val bundle = Bundle()
                    bundle.putString(AccountManager.KEY_ACCOUNT_NAME, user)
                    bundle.putString(AccountManager.KEY_ACCOUNT_TYPE, BuildConfig.APPLICATION_ID)
                    setAccountAuthenticatorResult(bundle)
                    finish()
                    startActivity(Intent(Application.context, UserDetailsActivity::class.java))
                }
                is Result.Error -> showErrorOnSnackbar(password, tokenResult.response.message())
                is Result.Exception -> showExceptionOnSnackbar(password, tokenResult.exception)
            }

            showProgress(false)
        }
    }

    private fun showProgress(show: Boolean) {
        login_progress.visibility = if (show) View.VISIBLE else View.GONE
        login_form.visibility = if (show) View.GONE else View.VISIBLE
    }
}
