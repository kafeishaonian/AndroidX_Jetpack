package com.example.module_fundamental.crash

import android.content.Context
import android.os.Process
import java.text.SimpleDateFormat
import java.util.Locale

class CrashHandler private constructor(context: Context) : Thread.UncaughtExceptionHandler {

    private val mContext = context
    private val info = mutableMapOf<String, String>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    companion object{
        @Volatile
        private var instance: CrashHandler? = null
        fun getInstance(context: Context): CrashHandler {
            return instance ?: synchronized(this) {
                instance ?: CrashHandler(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        handleException(e)
        // 给用户2秒阅读提示的时间
        Thread.sleep(2000)
        // 优雅退出
        Process.killProcess(Process.myPid())
        System.exit(1)
    }

    private fun handleException(e: Throwable) {

    }
}