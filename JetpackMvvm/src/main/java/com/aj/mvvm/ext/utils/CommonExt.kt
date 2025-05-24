package com.aj.mvvm.ext.utils

import android.content.ClipData
import android.content.Context
import android.provider.Settings
import android.text.Html
import android.text.Spanned
import android.view.View
import com.aj.mvvm.ext.view.clickNoRepeat

/**
 * 获取屏幕高度
 */
val Context.screenWidth
    get() = resources.displayMetrics.widthPixels

/**
 * 获取屏幕高度
 */
val Context.screenHeight
    get() = resources.displayMetrics.heightPixels


/**
 * 判断是否为空，并传入相关操作
 */
inline fun <reified T> T?.notNull(notNullAction: (T) -> Unit, nullAction: () -> Unit = {}) {
    if (this != null) {
        notNullAction.invoke(this)
    } else {
        nullAction.invoke()
    }
}

/**
 * dp值转换为px
 */
fun Context.dp2px(dp: Int) : Int {
    val scale = resources.displayMetrics.density
    return (dp * scale + 0.5f).toInt()
}

/**
 * px值转换成dp
 */
fun Context.px2dp(px: Int) : Int {
    val scale = resources.displayMetrics.density
    return (px / scale + 0.5f).toInt()
}


/**
 * dp值转换为px
 */
fun View.dp2px(dp: Int): Int {
    val scale = resources.displayMetrics.density
    return (dp * scale + 0.5f).toInt()
}

/**
 * px值转换成dp
 */
fun View.px2dp(px: Int): Int {
    val scale = resources.displayMetrics.density
    return (px / scale + 0.5f).toInt()
}

/**
 * 复制文本到粘贴板
 */

fun Context.copyToClipboard(text: String, label: String = "") {
    val clipData = ClipData.newPlainText(label, text)
    clipboardManager?.setPrimaryClip(clipData)
}

/**
 * 检查是否启用无障碍服务
 */
fun Context.checkAccessibilityServiceEnabled(serviceName: String): Boolean {
    val settingValue = Settings.Secure.getString(applicationContext.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false

    return settingValue.split(':').any {
        it.equals(serviceName, ignoreCase = true)
    }
}

/**
 * 设置点击事件
 */
fun setOnClick(vararg views: View?, onClick: (View) -> Unit) {
    views.forEach {
        it?.setOnClickListener { view->
            onClick.invoke(view)
        }
    }
}

/**
 * 设置防止重复点击事件
 */
fun setOnClickNoRepeat(vararg views: View?, interval: Long = 800, onClick: (View) -> Unit) {
    views.forEach {
        it?.clickNoRepeat(interval = interval) { view ->
            onClick.invoke(view)
        }
    }
}

fun String.toHtml(flag: Int = Html.FROM_HTML_MODE_LEGACY) : Spanned {
    return Html.fromHtml(this, flag)
}