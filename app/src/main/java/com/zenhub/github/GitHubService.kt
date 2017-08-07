package com.zenhub.github

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path

interface GitHubService {

    @GET("users/{username}/repos")
    fun listRepos(@Header("If-None-Match") etag: String?,
                  @Path("username") user: String): Call<List<Repository>>

    @GET("users/{username}/starred")
    fun listStarred(@Header("If-None-Match") etag: String?,
                    @Path("username") user: String): Call<List<Repository>>

    @GET("users/{username}")
    fun userDetails(@Header("If-None-Match") etag: String?,
                    @Path("username") user: String): Call<User>

    @GET("repos/{fullname}")
    fun repoDetails(@Header("If-None-Match") etag: String?,
                    @Path("fullname", encoded = true) fullname: String): Call<RepositoryDetails>

    @GET("repos/{fullname}/readme")
    @Headers("Accept: application/vnd.github.v3.html")
    fun repoReadme(@Header("If-None-Match") etag: String?,
                   @Path("fullname", encoded = true) fullname: String): Call<ResponseBody>

    @GET("repos/{fullname}/commits")
    fun commits(@Header("If-None-Match") etag: String?,
                @Path("fullname", encoded = true) fullname: String): Call<List<Commit>>

    @GET("repos/{fullname}/contents/{path}")
    fun repoContents(@Header("If-None-Match") etag: String?,
                     @Path("fullname", encoded = true) fullname: String,
                     @Path("path") path: String): Call<List<RepoContentEntry>>
}