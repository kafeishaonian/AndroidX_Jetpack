package com.aj.demo

import com.example.router.IFeed
import com.example.router.RouterProvider


@RouterProvider
class FeedImpl: IFeed{

    override fun getFeed(): String {
        return "成功了，你知道吗"
    }

}