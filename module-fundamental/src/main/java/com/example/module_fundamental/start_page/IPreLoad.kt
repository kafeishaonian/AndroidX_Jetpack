package com.example.module_fundamental.start_page

interface IPreLoad {

    fun tag(): String


    fun <T> notify(data: T)

    // 是否是一次性的，一次性表示：得到数据后就不再关注该数据
    // 非一次性表示，持续关注该数据，只要有更新，就通知数据给页面
    fun oneShot(): Boolean = true
}