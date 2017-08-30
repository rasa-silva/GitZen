package com.zenhub.github

import com.zenhub.Application
import com.zenhub.Application.Companion.GSON
import com.zenhub.auth.LoggedUser
import okhttp3.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import retrofit2.http.Headers
import java.text.SimpleDateFormat
import java.util.*

val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH)

val gitHubService = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(GsonConverterFactory.create(GSON))
        .client(OkHttpClient.Builder()
                .cache(Cache(Application.context.cacheDir, 1024 * 1024L).apply { evictAll() })
                .addInterceptor(OAuthTokenInterceptor())
                .addInterceptor(LoggingInterceptor())
                .build())
        .build()
        .create(GitHubService::class.java)

class OAuthTokenInterceptor : Interceptor {

    private val token: String? by lazy { LoggedUser.getToken() }

    override fun intercept(chain: Interceptor.Chain): Response {

        val request = if (token != null) {
            chain.request().newBuilder().addHeader("Authorization", "token $token").build()
        } else {
            chain.request()
        }

        return chain.proceed(request)
    }
}

interface GitHubService {

    @POST("authorizations")
    fun createToken(@Header("Authorization") authorization: String,
                    @Body tokenRequest: TokenRequest): Call<TokenResponse>

    @GET("user/repos")
    fun listRepos(): Call<List<Repository>>

    @GET
    fun listReposPaginate(@Url url: String): Call<List<Repository>>

    @GET("user/starred")
    fun listStarred(): Call<List<Repository>>

    @GET
    fun listStarredPaginate(@Url url: String): Call<List<Repository>>

    @GET("user")
    fun userDetails(): Call<User>

    @GET("repos/{fullname}")
    fun repoDetails(@Path("fullname", encoded = true) fullname: String): Call<RepositoryDetails>

    @GET("repos/{fullname}/readme")
    @Headers("Accept: application/vnd.github.v3.html")
    fun repoReadme(@Path("fullname", encoded = true) fullname: String): Call<ResponseBody>

    @GET("repos/{fullname}/commits")
    fun commits(@Path("fullname", encoded = true) fullname: String): Call<List<Commit>>

    @GET
    fun commitsPaginate(@Url url: String): Call<List<Commit>>

    @GET("repos/{fullname}/contents/{path}")
    fun repoContents(@Path("fullname", encoded = true) fullname: String,
                     @Path("path", encoded = true) path: String): Call<List<RepoContentEntry>>

    @GET("repos/{fullname}/commits/{sha}")
    fun commit(@Path("fullname", encoded = true) fullname: String,
               @Path("sha") sha: String): Call<CommitDetails>
}
