package com.zenhub

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.text.SimpleDateFormat
import java.util.*

val STUBBED_USER = "rasa-silva"
val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)

interface GitHubService {

    @GET("users/{username}/starred")
    fun listStarred(@Path("username") user: String): Call<List<Repository>>

    @GET("users/{username}")
    fun userDetails(@Path("username") user: String): Call<User>
}

class Repository(val name: String, val full_name: String,
                 val description: String, val pushed_at: String,
                 val stargazers_count: Int, val language: String)

class User(val login: String, val avatar_url: String, val name: String,
           val public_repos: Int, val public_gists: Int,
           val followers: Int, val following: Int,
           val created_at: String)


val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

val gitHubService = retrofit.create(GitHubService::class.java)

fun getStarredRepos(callback: Callback<List<Repository>>) {
    gitHubService.listStarred(STUBBED_USER).enqueue(callback)
}

fun getUserDetails(callback: Callback<User>) {
    gitHubService.userDetails(STUBBED_USER).enqueue(callback)
}