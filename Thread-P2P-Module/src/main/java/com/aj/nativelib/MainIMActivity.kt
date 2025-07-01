package com.aj.nativelib

import android.R
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.aj.nativelib.JSONProcessor.ChatMessage
import com.aj.nativelib.JSONProcessor.LoginMessage
import com.aj.nativelib.JSONProcessor.SystemMessage
import org.json.JSONException
import org.json.JSONObject


class MainIMActivity: AppCompatActivity(), IMClient.Callback {

    private var imClient: IMClient? = null
    private val currentUserId = "user123"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)

        imClient = IMClient()
        imClient?.init()
        imClient?.registerCallbacks(this)


        // 连接服务器
        imClient?.connect("im.example.com", 8080)


        // 发送消息按钮
//        findViewById<View>(R.id.btn_send).setOnClickListener { v: View? ->
//            val receiverEdit = findViewById<EditText>(R.id.et_receiver)
//            val messageEdit = findViewById<EditText>(R.id.et_message)


            // 构建JSON消息
            val message = ChatMessage()
            message.sender = currentUserId
//            message.receiver = receiverEdit.text.toString()
            message.receiver = ""
//            message.content = messageEdit.text.toString()
            message.content = ""
            message.timestamp = System.currentTimeMillis()
            imClient?.sendRawMessage(message.toJson())
//        }
    }

    override fun onStateChanged(state: Int) {
        runOnUiThread {
            when (state) {
                IMClient.STATE_CONNECTED -> {
                    sendLoginMessage()
                    updateStatus("Connected")
                }

                IMClient.STATE_DISCONNECTED -> updateStatus("Disconnected")
                IMClient.STATE_CONNECTING -> updateStatus("Connecting...")
                IMClient.STATE_RECONNECTING -> updateStatus("Reconnecting...")
            }
        }
    }

    override fun onMessageReceived(rawJson: String?) {
        runOnUiThread {
            try {
                // 解析消息类型
                val json = JSONObject(rawJson)
                val type = json.getString("type")

                when (type) {
                    "chat" -> {
                        val chatMsg =
                            ChatMessage.fromJson(rawJson)
                        addMessage(chatMsg.sender + ": " + chatMsg.content)
                    }

                    "system" -> {
                        val sysMsg =
                            SystemMessage.fromJson(rawJson)
                        showSystemMessage(sysMsg.content)
                    }

                    "login_response" -> handleLoginResponse(rawJson)
                }
            } catch (e: JSONException) {
                Log.e("IMClient", "JSON parse error", e)
            }
        }
    }

    private fun sendLoginMessage() {
        // 构建登录消息
        val loginMsg = LoginMessage()
        loginMsg.userId = currentUserId
        loginMsg.device = "Android"
        loginMsg.token = getAuthToken() // 从本地存储获取token

        imClient!!.sendRawMessage(loginMsg.toJson())
    }


    private fun handleLoginResponse(rawJson: String?) {
        try {
            val json = JSONObject(rawJson)
            if (json.getBoolean("success")) {
                Log.d("IMClient", "Login success")
            } else {
                val error = json.getString("error")
                showError("Login failed: $error")
            }
        } catch (e: JSONException) {
            Log.e("IMClient", "Login response parse error", e)
        }
    }

    override fun onErrorOccurred(errorCode: Int, errorMessage: String?) {
        runOnUiThread {
            val error = when (errorCode) {
                IMClient.ERROR_NETWORK -> "Network error: $errorMessage"
                IMClient.ERROR_TIMEOUT -> "Timeout: $errorMessage"
                else -> "Server error: $errorMessage"
            }
            showError(error)
        }
    }

    private fun updateStatus(status: String) {
//        (findViewById<View>(R.id.tv_status) as TextView).text = status
    }

    private fun addMessage(message: String) {
//        val messageView = findViewById<TextView>(R.id.tv_messages)
        val messageView = TextView(this)
        messageView.append(
            """
            
            $message
            """.trimIndent()
        )
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showSystemMessage(message: String?) {
        Toast.makeText(this, "System: $message", Toast.LENGTH_LONG).show()
    }

    private fun getAuthToken(): String {
        // 从SharedPreferences获取token
        return "user_auth_token_123456"
    }
}