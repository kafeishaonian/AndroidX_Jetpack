package com.aj.nativelib

import org.json.JSONException
import org.json.JSONObject


object JSONProcessor {

    class LoginMessage {
        var userId: String? = null
        var device: String? = null
        var token: String? = null

        fun toJson(): String {
            try {
                val json = JSONObject()
                json.put("type", "login")
                json.put("user_id", userId)
                json.put("device", device)
                json.put("token", token)
                return json.toString()
            } catch (e: JSONException) {
                throw RuntimeException("Error creating login JSON", e)
            }
        }

        companion object {
            fun fromJson(jsonStr: String): LoginMessage {
                try {
                    val json = JSONObject(jsonStr)
                    val message = LoginMessage()
                    message.userId = json.getString("user_id")
                    message.device = json.getString("device")
                    message.token = json.getString("token")
                    return message
                } catch (e: JSONException) {
                    throw RuntimeException("Error parsing login JSON", e)
                }
            }
        }
    }

    class ChatMessage {
        var sender: String? = null
        var receiver: String? = null
        var content: String? = null
        var timestamp: Long = 0

        fun toJson(): String {
            try {
                val json = JSONObject()
                json.put("type", "chat")
                json.put("sender", sender)
                json.put("receiver", receiver)
                json.put("content", content)
                json.put("timestamp", timestamp)
                return json.toString()
            } catch (e: JSONException) {
                throw RuntimeException("Error creating chat JSON", e)
            }
        }

        companion object {
            fun fromJson(jsonStr: String?): ChatMessage {
                try {
                    val json = JSONObject(jsonStr)
                    val message = ChatMessage()
                    message.sender = json.getString("sender")
                    message.receiver = json.getString("receiver")
                    message.content = json.getString("content")
                    message.timestamp = json.getLong("timestamp")
                    return message
                } catch (e: JSONException) {
                    throw RuntimeException("Error parsing chat JSON", e)
                }
            }
        }
    }

    class SystemMessage {
        var messageType: String? = null
        var content: String? = null

        companion object {
            fun fromJson(jsonStr: String?): SystemMessage {
                try {
                    val json = JSONObject(jsonStr)
                    val message = SystemMessage()
                    message.messageType = json.getString("type")
                    message.content = json.getString("content")
                    return message
                } catch (e: JSONException) {
                    throw RuntimeException("Error parsing system JSON", e)
                }
            }
        }
    }

}