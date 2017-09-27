package com.zenhub.github

import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import okhttp3.*
import java.io.IOException

enum class IssueState { OPEN, CLOSED }

class PageInfo(val hasNextPage: Boolean, val endCursor: String)

class Label(val color: String, val name: String)

class LabelConnection(val nodes: List<Label>)

class Issue(val number: Int, val title: String,
            val state: IssueState, val body: String,
            val updatedAt: String, val labels: LabelConnection)

class IssueConnection(val totalCount: Int, val pageInfo: PageInfo, val nodes: List<Issue>)

class RepositoryType(val issues: IssueConnection)

class RepoWithIssues(val repository: RepositoryType)

class RepoIssuesResponse(val data: RepoWithIssues)

suspend fun fetchRepoIssues(owner: String, repo: String, from: String = ""): Result<RepoIssuesResponse> {

    val cursor = if(from.isBlank()) "" else """, after: \"$from\""""

    val query = """{
"query": "query {
  repository(owner: \"$owner\", name:\"$repo\") {
    issues(first:20 $cursor) {
      totalCount
      pageInfo {
        hasNextPage
        endCursor
      }
      nodes {
        number
        title
        state
        updatedAt
        labels(first:20) {
          nodes {
            name
            color
          }
        }
        body
      }
    }
  }
}"
}
"""

    val response = doRequest(query)
    val json = response.body()?.string()
    return if (response.isSuccessful)
        Ok(GSON.fromJson(json, RepoIssuesResponse::class.java))
    else
        Fail(json ?: response.message())
}

const private val BASE_URL = "https://api.github.com/graphql"

private suspend fun doRequest(query: String): Response {
    val trimmedQuery = query.replace('\n', ' ').trimMargin()
    val body = RequestBody.create(MediaType.parse("application/json"), trimmedQuery)
    val request = Request.Builder().url(BASE_URL).post(body).build()
    return okHttpClient.newCall(request).await()
}

suspend fun Call.await(): Response {
    return suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }

            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(e)
            }
        })

        continuation.invokeOnCompletion {
            if (continuation.isCancelled)
                try {
                    cancel()
                } catch (ex: Throwable) {
                    //Ignore cancel exception
                }
        }
    }
}

sealed class Result<out T>
class Ok<out T>(val value: T) : Result<T>()
class Fail<out T>(val error: String) : Result<T>()