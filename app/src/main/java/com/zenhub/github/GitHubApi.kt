package com.zenhub.github

import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import com.zenhub.Application
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

val STUBBED_USER = "rasa-silva"
val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)

val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

val gitHubService: GitHubService = retrofit.create(GitHubService::class.java)

fun showGitHubApiError(errorBody: ResponseBody?, parent: View) {
    val error = errorBody?.string()
    Log.e(Application.LOGTAG, "Failed response: $error")
    val errorMessage = Application.GSON.fromJson<ErrorMessage>(error, ErrorMessage::class.java)
    val snackbar = Snackbar.make(parent, errorMessage.message, Snackbar.LENGTH_INDEFINITE)
    snackbar.view.setBackgroundColor(0xfffb4934.toInt())
    snackbar.show()
}

object GitHubApi {
    private val service: GitHubService = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GitHubService::class.java)

    private val callbacks = mutableMapOf<RequestType, OnApiResponse<*>>()

    enum class RequestType {REPO_DETAILS, REPO_README}

    fun repoDetails(repoName: String, parentView: View,
                    block: (response: Response<RepositoryDetails>, rootView: View) -> Unit) {
        val callback = callbacks[RequestType.REPO_DETAILS] ?: OnApiResponse(parentView, block)
        callbacks[RequestType.REPO_DETAILS] = callback
        service.repoDetails(callback.etag, repoName).enqueue(callback as Callback<RepositoryDetails>)
    }

    fun readMeData(repoName: String, parentView: View,
                   block: (response: Response<ResponseBody>, rootView: View) -> Unit) {
        val callback = callbacks[RequestType.REPO_README] ?: OnApiResponse(parentView, block)
        callbacks[RequestType.REPO_README] = callback
        service.repoReadme(callback.etag, repoName).enqueue(callback as Callback<ResponseBody>)
    }
}

class OnApiResponse<T>(private val parentView: View,
                       private val block: (response: Response<T>, rootView: View) -> Unit) : Callback<T> {

    var etag: String? = null

    override fun onResponse(call: Call<T>, response: Response<T>) {
        Log.d(Application.LOGTAG, "Response for ${call.request().url()}")
        etag = response.headers()["ETag"]
        when {
            response.code() == 304 -> Unit
            !response.isSuccessful -> showGitHubApiError(response.errorBody(), parentView)
            else -> block.invoke(response, parentView)
        }
    }

    override fun onFailure(call: Call<T>, t: Throwable) {
        Log.d(Application.LOGTAG, "Failed: $t")
    }
}