package com.zenhub.github

import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import com.zenhub.Application
import okhttp3.Cache
import okhttp3.OkHttpClient
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

object GitHubApi {
    private val service: GitHubService = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder()
                    .cache(Cache(Application.context.cacheDir, 1024 * 1024L))
                    .addInterceptor(LoggingInterceptor())
                    .build())
            .build()
            .create(GitHubService::class.java)

    fun userDetails(user: String, parentView: View,
                    block: (response: User?, rootView: View) -> Unit) {
        service.userDetails(user).enqueue(OnApiResponse(parentView, block))
    }

    fun repoDetails(repoName: String, parentView: View,
                    block: (response: RepositoryDetails?, rootView: View) -> Unit) {
        service.repoDetails(repoName).enqueue(OnApiResponse(parentView, block))
    }

    fun readMeData(repoName: String, parentView: View,
                   block: (response: ResponseBody?, rootView: View) -> Unit) {
        service.repoReadme(repoName).enqueue(OnApiResponse(parentView, block))
    }

    fun ownRepos(parentView: View,
                 block: (response: List<Repository>?, rootView: View) -> Unit) {
        service.listRepos(STUBBED_USER).enqueue(OnApiResponse(parentView, block))
    }

    fun starredRepos(user: String, parentView: View,
                     block: (response: List<Repository>?, rootView: View) -> Unit) {
        service.listStarred(user).enqueue(OnApiResponse(parentView, block))
    }

    fun commits(repoName: String, parentView: View,
                block: (response: List<Commit>?, rootView: View) -> Unit) {
        service.commits(repoName).enqueue(OnApiResponse(parentView, block))
    }

    fun repoContents(repoName: String, path: String, parentView: View,
                     block: (response: List<RepoContentEntry>?, rootView: View) -> Unit) {
        service.repoContents(repoName, path).enqueue(OnApiResponse(parentView, block))
    }

}

class OnApiResponse<T>(private val parentView: View,
                       private val block: (response: T?, rootView: View) -> Unit) : Callback<T> {

    override fun onResponse(call: Call<T>, response: Response<T>) {
        if (response.isSuccessful) block.invoke(response.body(), parentView)
        else showGitHubApiError(response.errorBody(), parentView)
    }

    override fun onFailure(call: Call<T>, t: Throwable) {
        Log.d(Application.LOGTAG, "Failed: $t")
    }

    private fun showGitHubApiError(errorBody: ResponseBody?, parent: View) {
        val error = errorBody?.string()
        Log.e(Application.LOGTAG, "Failed response: $error")
        val errorMessage = Application.GSON.fromJson<ErrorMessage>(error, ErrorMessage::class.java)
        val snackbar = Snackbar.make(parent, errorMessage.message, Snackbar.LENGTH_INDEFINITE)
        snackbar.view.setBackgroundColor(0xfffb4934.toInt())
        snackbar.show()
    }
}