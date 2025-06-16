package com.aj.mvvm.ext.utils

import android.util.Log
import androidx.annotation.IntDef
import kotlin.intArrayOf

const val TAG = "JetpackMvvm"

var jetpackMvvmLog = true


const val V = 0
const val D = 1
const val I = 2
const val W = 3
const val E = 4
@Retention(AnnotationRetention.SOURCE)
@IntDef(value = [V, D, I, W, E])
annotation class LEVEL


fun String.logv(tag: String = TAG) = log(V, tag, this)

fun String.logd(tag: String = TAG) = log(D, tag, this)

fun String.logi(tag: String = TAG) = log(I, tag, this)

fun String.logw(tag: String = TAG) = log(W, tag, this)

fun String.loge(tag: String = TAG) = log(E, tag, this)


private fun log(@LEVEL level: Int, tag: String, message: String) {
    if (!jetpackMvvmLog) return
    when (level) {
        V -> Log.v(tag, message)
        D -> Log.d(tag, message)
        I -> Log.i(tag, message)
        W -> Log.w(tag, message)
        E -> Log.e(tag, message)
    }
}