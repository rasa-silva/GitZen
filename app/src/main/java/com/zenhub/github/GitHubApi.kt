package com.zenhub.github

import com.google.gson.GsonBuilder
import com.zenhub.Application
import com.zenhub.core.LoggingInterceptor
import com.zenhub.core.OAuthTokenInterceptor
import com.zenhub.github.mappings.*
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

val GSON = GsonBuilder()
        .registerTypeAdapter(ReceivedEvent::class.java, EventDeserializer())
        .create()

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



interface GitHubService {

    @POST("authorizations")
    fun createToken(@Header("Authorization") authorization: String,
                    @Body tokenRequest: TokenRequest): Call<TokenResponse>

    @GET("users/{user}")
    fun userDetails(@Path("user") user: String): Call<User>

    @GET("users/{user}/received_events")
    fun receivedEvents(@Path("user") user: String): Call<List<ReceivedEvent>>

    @GET
    fun receivedEventsPaginate(@Url url: String): Call<List<ReceivedEvent>>

    @GET("users/{user}/repos")
    fun listRepos(@Path("user") user: String): Call<List<Repository>>

    @GET
    fun listReposPaginate(@Url url: String): Call<List<Repository>>

    @GET("users/{user}/starred")
    fun listStarred(@Path("user") user: String): Call<List<Repository>>

    @GET
    fun listStarredPaginate(@Url url: String): Call<List<Repository>>

    @GET("users/{user}/followers")
    fun listFollowers(@Path("user") user: String): Call<List<User>>

    @GET("users/{user}/following")
    fun listFollowing(@Path("user") user: String): Call<List<User>>

    @GET
    fun listUsersPaginate(@Url url: String): Call<List<User>>

    @GET("user/starred/{fullname}")
    fun isStarred(@Path("fullname", encoded = true) fullname: String): Call<Void>

    @PUT("user/starred/{fullname}")
    @Headers("Content-Length: 0")
    fun starRepo(@Path("fullname", encoded = true) fullname: String): Call<Void>

    @DELETE("user/starred/{fullname}")
    fun unstarRepo(@Path("fullname", encoded = true) fullname: String): Call<Void>

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

    @GET("search/repositories")
    fun searchRepos(@Query("q") query: String): Call<RepositorySearch>

    @GET
    fun searchReposPaginate(@Url url: String): Call<RepositorySearch>

    @GET("search/users")
    fun searchUsers(@Query("q") query: String): Call<UserSearch>

    @GET
    fun searchUsersPaginate(@Url url: String): Call<UserSearch>

    @GET("user/following/{user}")
    fun isFollowing(@Path("user") user: String): Call<Void>

    @PUT("user/following/{user}")
    fun follow(@Path("user") user: String): Call<Void>

    @DELETE("user/following/{user}")
    fun unfollow(@Path("user") user: String): Call<Void>

    @GET("users/{user}/gists")
    fun listGists(@Path("user") user: String): Call<List<Gist>>

    @GET
    fun listGistsPaginate(@Url url: String): Call<List<Gist>>

    @GET
    fun gist(@Url url: String): Call<Gist>

    @DELETE("gists/{id}")
    fun deleteGist(@Path("id") id: String): Call<Void>

    @POST("/gists")
    fun createGist(@Body gist: NewGist): Call<Void>
}
