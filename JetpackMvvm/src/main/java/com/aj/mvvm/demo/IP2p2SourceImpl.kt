package com.aj.mvvm.demo

import com.aj.nativelib.IP2pSource
import com.example.router.RouterProvider

@RouterProvider
class IP2p2SourceImpl: IP2pSource {
    override fun getPublicIP(): String {
        return "Ip is null"
    }
}