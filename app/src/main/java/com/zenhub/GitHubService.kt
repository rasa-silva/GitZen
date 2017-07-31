package com.zenhub

import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import java.text.SimpleDateFormat
import java.util.*

val STUBBED_USER = "rasa-silva"
val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)

interface GitHubService {

    @GET("users/{username}/repos")
    fun listRepos(@Path("username") user: String): Call<List<Repository>>

    @GET("users/{username}/starred")
    fun listStarred(@Path("username") user: String): Call<List<Repository>>

    @GET("users/{username}")
    fun userDetails(@Path("username") user: String): Call<User>

    @GET("repos/{fullname}")
    fun repoDetails(@Path("fullname", encoded = true) fullname: String): Call<RepositoryDetails>

    @GET("repos/{fullname}/readme")
    @Headers("Accept: application/vnd.github.v3.html")
    fun repoReadme(@Path("fullname", encoded = true) fullname: String): Call<ResponseBody>

    @GET("repos/{fullname}/commits")
    fun commits(@Path("fullname", encoded = true) fullname: String): Call<List<Commit>>

    @GET("repos/{fullname}/contents/{path}")
    fun repoContents(@Path("fullname", encoded = true) fullname: String,
                     @Path("path") path: String): Call<List<RepoContentEntry>>
}

class Repository(val name: String, val full_name: String,
                 val description: String, val pushed_at: String,
                 val stargazers_count: Int, val language: String)

class RepositoryDetails(val name: String, val full_name: String,
                 val description: String, val pushed_at: String,
                 val stargazers_count: Int, val language: String)

class User(val login: String, val avatar_url: String, val name: String,
           val public_repos: Int, val public_gists: Int,
           val followers: Int, val following: Int,
           val created_at: String)

class Committer(val login: String, val avatar_url: String)
class CommitInfo(val message: String, val comment_count: Int, val committer: CommitCommitter)
class CommitCommitter(val date: String)
class Commit(val commit: CommitInfo, val committer: Committer?)

class RepoContentEntry(val name: String, val path: String, val size: Int, val type: String)

class ErrorMessage(val message: String)

val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

val gitHubService: GitHubService = retrofit.create(GitHubService::class.java)

val retrofitRaw: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .build()

val gitHubServiceRaw: GitHubService = retrofitRaw.create(GitHubService::class.java)

fun showGitHubApiError(errorBody: ResponseBody?, parent: View) {
    val error = errorBody?.string()
    Log.e(Application.LOGTAG, "Failed response: $error")
    val errorMessage = Application.GSON.fromJson<ErrorMessage>(error, ErrorMessage::class.java)
    val snackbar = Snackbar.make(parent, errorMessage.message, Snackbar.LENGTH_INDEFINITE)
    snackbar.view.setBackgroundColor(0xfffb4934.toInt())
    snackbar.show()
}