package com.aj.nativelib

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


object PublicIPFetcher {



    val IP_SERVICES: Array<String?> = arrayOf<String?>(
        "https://icanhazip.com",  // 首选HTTPS接口
        "https://api.ipify.org",  // 备用接口1
        "https://checkip.amazonaws.com" // 备用接口2
    )

    fun getPublicIP(): String {
        for (serviceUrl in IP_SERVICES) {
            try {
                val url = URL(serviceUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.setConnectTimeout(5000) // 设置超时
                connection.setReadTimeout(5000)

                val reader = BufferedReader(
                    InputStreamReader(connection.getInputStream())
                )
                val ip = reader.readLine().trim { it <= ' ' }  // 去除空格和换行
                reader.close()
                return ip
            } catch (e: IOException) {
                e.printStackTrace() // 输出错误日志
            }
        }
        return "Failed to fetch IP: null"
    }

}