package com.aj.nativelib

import android.util.Log

object ThreadDemo {

    init {
        System.loadLibrary("nativelib")
    }

    private var connectCallback: ((String) -> Unit)? = null

    fun setP2PCallback(callback: (String) -> Unit) {
        connectCallback = callback
    }


    external fun initP2P(tcpPort: Int, udpPort: Int): Boolean
    external fun sendData(ip: String, data: String)
    external fun destroyP2P()
    external fun requestConnectToPeer(ip: String, port: Int)

    fun onDataReceived(ip: String, data: String) {
        Log.e("LogLogLog", "-----------> ip: = $ip,   data:= $data")
    }

    fun onPeerConnected(ip: String) {
        Log.e("LogLogLog", "onPeerConnected ip:= $ip")
        connectCallback?.invoke(ip)
    }

    fun onPeerDisconnected(ip: String) {
        Log.e("LogLogLog", "onPeerDisconnectedCallback ip:= $ip")
    }
}