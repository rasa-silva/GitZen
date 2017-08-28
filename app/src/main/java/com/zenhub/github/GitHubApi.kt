package com.zenhub.github

import com.zenhub.Application
import com.zenhub.Application.Companion.GSON
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.text.SimpleDateFormat
import java.util.*

val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)

val gitHubService: GitHubService = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(GsonConverterFactory.create(GSON))
        .client(OkHttpClient.Builder()
                .cache(Cache(Application.context.cacheDir, 1024 * 1024L).apply { evictAll() })
                .addInterceptor(LoggingInterceptor())
                .build())
        .build()
        .create(GitHubService::class.java)

interface GitHubService {

    @POST("authorizations")
    fun createToken(@Header("Authorization") authorization: String,
                    @Body tokenRequest: TokenRequest): Call<TokenResponse>

    @GET("users/{username}/repos")
    fun listRepos(@Path("username") user: String): Call<List<Repository>>

    @GET("users/{username}/starred")
    fun listStarred(@Path("username") user: String): Call<List<Repository>>

    @GET
    fun listStarredPaginate(@Url url: String): Call<List<Repository>>

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
                     @Path("path", encoded = true) path: String): Call<List<RepoContentEntry>>

    @GET("repos/{fullname}/commits/{sha}")
    fun commit(@Path("fullname", encoded = true) fullname: String,
               @Path("sha") sha: String): Call<CommitDetails>
}
