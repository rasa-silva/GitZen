package com.zenhub.github

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class TokenRequest(val client_id: String, val client_secret: String, val scopes: List<String>, val note: String)
class TokenResponse(val token: String)

class Repository(val name: String, val full_name: String,
                 val description: String, val pushed_at: String,
                 val stargazers_count: Int, val language: String?)

class RepositoryDetails(val name: String, val description: String, val stargazers_count: Int,
                        val homepage: String?, val html_url: String,
                        val pushed_at: String, val language: String, val size: Long)

class User(val login: String, val avatar_url: String, val name: String,
           val public_repos: Int, val public_gists: Int,
           val followers: Int, val following: Int,
           val created_at: String)

class Committer(val avatar_url: String)
class CommitInfo(val message: String, val committer: CommitCommitter)
class CommitCommitter(val name: String, val date: String)
class Commit(val sha: String, val commit: CommitInfo, val committer: Committer?)

class CommitFile(val filename: String, val patch: String)
class CommitDetails(val commit: CommitInfo, val files: List<CommitFile>)

class RepoContentEntry(val name: String, val path: String, val size: Long, val type: String, val download_url: String)

class ErrorMessage(val message: String)

// Events API
class ReceivedEvent(val type: String, val actor: EventActor, val repo: String, val created_at: String, val payload: EventPayload)

class EventActor(val display_login: String, val avatar_url: String)
sealed class EventPayload
class WatchEvent(val action: String) : EventPayload()
class PullRequestEvent(val action: String, val number: Int) : EventPayload()
class IssuesEvent(val action: String, val number: Int) : EventPayload()
object ForkEvent : EventPayload()
object UnsupportedEvent : EventPayload()

class EventDeserializer : JsonDeserializer<ReceivedEvent> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ReceivedEvent {
        val event = json.asJsonObject

        val actor = event["actor"].asJsonObject
        val eventActor = EventActor(actor["display_login"].asString, actor["avatar_url"].asString)

        val repo = event["repo"].asJsonObject["name"].asString
        val created_at = event["created_at"].asString

        val type = event["type"].asString
        val payload = event["payload"].asJsonObject
        val eventPayload = when (type) {
            "WatchEvent" -> WatchEvent(payload["action"].asString)
            "PullRequestEvent" -> {
                PullRequestEvent(payload["action"].asString, payload["number"].asInt)
            }
            "ForkEvent" -> ForkEvent
            "IssuesEvent" -> {
                IssuesEvent(payload["action"].asString, payload["issue"].asJsonObject["number"].asInt)
            }
            else -> UnsupportedEvent
        }

        return ReceivedEvent(type, eventActor, repo, created_at, eventPayload)
    }
}

//Search
class RepositorySearch(val total_count: Int, val items: List<Repository>)