package com.zenhub

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


val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

val gitHubService: GitHubService = retrofit.create(GitHubService::class.java)

val retrofitRaw: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .build()

val gitHubServiceRaw: GitHubService = retrofitRaw.create(GitHubService::class.java)
