package com.zenhub.github.mappings

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class ReceivedEvent(val type: String, val actor: EventActor, val repo: String, val created_at: String, val payload: EventPayload)

class EventActor(val display_login: String, val avatar_url: String)

sealed class EventPayload
class WatchEvent(val action: String) : EventPayload()
class PullRequestEvent(val action: String, val number: Int) : EventPayload()
class IssuesEvent(val action: String, val number: Int) : EventPayload()
object ForkEvent : EventPayload()
class CreateEvent(val ref_type: String, val ref: String) : EventPayload()
class DeleteEvent(val ref_type: String, val ref: String) : EventPayload()
class IssueCommentEvent(val action: String, val number: Int): EventPayload()
class PushEvent(val ref: String): EventPayload()
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
            "CreateEvent" -> {
                val ref = payload["ref"]
                val refString = if (ref.isJsonNull) "" else ref.asString
                CreateEvent(payload["ref_type"].asString, refString)
            }
            "DeleteEvent" -> {
                val ref = payload["ref"]
                val refString = if (ref.isJsonNull) "" else ref.asString
                DeleteEvent(payload["ref_type"].asString, refString)
            }
            "IssueCommentEvent" ->  {
                IssueCommentEvent(payload["action"].asString,
                        payload["issue"].asJsonObject["number"].asInt)
            }
            "PushEvent" -> PushEvent(payload["ref"].asString)
            else -> UnsupportedEvent
        }

        return ReceivedEvent(type, eventActor, repo, created_at, eventPayload)
    }
}
