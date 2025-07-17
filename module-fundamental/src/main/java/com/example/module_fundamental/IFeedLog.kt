package com.example.module_fundamental

import com.example.router.log.annotations.ClickEvent
import com.example.router.log.annotations.EventModule
import com.example.router.log.annotations.Param
import com.example.router.log.annotations.ParamMap
import com.example.router.log.annotations.ShowEvent

@EventModule
interface IFeedLog {

    @ShowEvent(eventId = "10023", page = "page.home", action = "show.animation")
    fun showHomePage(@Param(key = "requestId") id: String)

    @ClickEvent(eventId = "10024", page = "page.home", action = "click.package")
    fun clickProfile(@ParamMap map: Map<String, Any>)

}