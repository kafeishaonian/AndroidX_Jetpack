package com.example.module_fundamental

import android.app.Application

object MyApplication: Application() {

    private var mContext: Application? = null

    override fun onCreate() {
        super.onCreate()
        mContext = this
    }

    fun getApplication() = mContext

}