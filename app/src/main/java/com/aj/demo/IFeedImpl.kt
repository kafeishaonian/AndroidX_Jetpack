package com.aj.demo

import android.util.Log
import com.example.module_fundamental.IFeed
import com.example.router.RouterProvider

@RouterProvider
class IFeedImpl: IFeed {
    override fun demo() {
        Log.e("Log", "Log")
    }
}