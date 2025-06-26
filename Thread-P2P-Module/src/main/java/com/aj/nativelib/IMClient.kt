package com.aj.nativelib

class IMClient {

    init {
        System.loadLibrary("nativelib")
    }


    companion object{
        const val STATE_DISCONNECTED: Int = 0
        const val STATE_CONNECTING: Int = 1
        const val STATE_CONNECTED: Int = 2
        const val STATE_RECONNECTING: Int = 3

        // 错误码常量
        const val ERROR_NETWORK: Int = 0
        const val ERROR_TIMEOUT: Int = 1
        const val ERROR_SERVER: Int = 2
    }

    interface Callback {
        fun onStateChanged(state: Int)
        fun onMessageReceived(rawJson: String?)
        fun onErrorOccurred(errorCode: Int, errorMessage: String?)
    }

    // JNI方法
    private external fun nativeInit()
    private external fun nativeConnect(host: String, port: Int)
    private external fun nativeDisconnect()
    private external fun nativeSendRawMessage(rawJson: String)
    private external fun nativeRegisterCallbacks(callback: Callback)

    fun init() {
        nativeInit()
    }

    fun connect(host: String, port: Int) {
        nativeConnect(host, port)
    }

    fun disconnect() {
        nativeDisconnect()
    }

    fun sendRawMessage(rawJson: String) {
        nativeSendRawMessage(rawJson)
    }

    fun registerCallbacks(callback: Callback) {
        nativeRegisterCallbacks(callback)
    }

}