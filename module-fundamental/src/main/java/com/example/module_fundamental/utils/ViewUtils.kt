package com.example.module_fundamental.utils

import android.content.Context
import android.graphics.Rect
import android.view.TouchDelegate
import android.view.View
import com.example.module_fundamental.MyApplication

fun Number.dp2px(context: Context): Int {
    return ScreenUtil.dp2px(context, toFloat())
}

fun Number.sp2px(context: Context): Int {
    return ScreenUtil.sp2px(context, toFloat())
}

fun Number.px2dp(context: Context): Int {
    return ScreenUtil.px2dp(context, toFloat())
}

fun Number.px2sp(context: Context): Int {
    return ScreenUtil.px2sp(context, toFloat())
}


fun View.expandTouchView(expandSize: Int = 10.dp2px(MyApplication.applicationContext)) {
    val parentView = (parent as? View)
    parentView?.post {
        val rect = Rect()
        getHitRect(rect)
        rect.left -= expandSize
        rect.top -= expandSize
        rect.right += expandSize
        rect.bottom += expandSize
        parentView.touchDelegate = TouchDelegate(rect, this)
    }
}