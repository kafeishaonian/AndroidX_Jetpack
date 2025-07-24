package com.example.module_fundamental.utils

import android.graphics.Rect
import android.view.TouchDelegate
import android.view.View
import com.example.module_fundamental.MyApplication

fun Number.dp2px(): Int {
    return ScreenUtil.dp2px(MyApplication.getApplication(), toFloat())
}

fun Number.sp2px(): Int {
    return ScreenUtil.sp2px(MyApplication.getApplication(), toFloat())
}

fun Number.px2dp(): Int {
    return ScreenUtil.px2dp(MyApplication.getApplication(), toFloat())
}

fun Number.px2sp(): Int {
    return ScreenUtil.px2sp(MyApplication.getApplication(), toFloat())
}


fun View.expandTouchView(expandSize: Int = 10.dp2px()) {
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