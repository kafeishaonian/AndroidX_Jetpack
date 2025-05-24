package com.aj.demo

import com.aj.nativelib.IP2pSource
import com.example.router.RouterProvider

@RouterProvider
class IP2pSourceImpl: IP2pSource {
    override fun getPublicIP(): String {
        return "127.0.0.1"
    }
}