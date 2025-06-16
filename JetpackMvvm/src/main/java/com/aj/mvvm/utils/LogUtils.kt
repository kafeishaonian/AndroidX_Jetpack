package com.aj.mvvm.utils

import android.util.Log
import com.aj.mvvm.ext.utils.jetpackMvvmLog

object LogUtils {

    private const val DEFAULT_TAG = "JetpackMvvm"

    fun debugInfo(tag: String?, msg: String?) {
        if (!jetpackMvvmLog) {
            return
        }
        msg?.let {
            Log.d(tag, msg)
        }
    }


    fun debugInfo(msg: String?) {
        debugInfo(DEFAULT_TAG, msg)
    }

    fun warnInfo(tag: String?, msg: String?) {
        if (!jetpackMvvmLog) {
            return
        }
        msg?.let {
            Log.w(tag, msg)
        }
    }

    fun warnInfo(msg: String?) {
        warnInfo(DEFAULT_TAG, msg)
    }

    fun debugLongInfo(tag: String?, msg: String?) {
        if (!jetpackMvvmLog) return

        msg?.let {
            val m = it.trim { it <= ' ' }
            var index = 0
            val maxLength = 3500
            var sub: String

            while (index < m.length) {
                sub = if (m.length < index + maxLength) {
                    msg.substring(index)
                } else {
                    msg.substring(index, index + maxLength)
                }
                index += maxLength
                Log.d(tag, sub.trim { it <= ' ' })
            }
        }
    }

    fun debugLongInfo(msg: String?) {
        debugLongInfo(DEFAULT_TAG, msg)
    }
}