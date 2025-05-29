package com.aj.mvvm.utils

import android.app.Activity
import android.content.Intent
import androidx.fragment.app.FragmentActivity

object ActivityMessenger {

    private var sRequestCode = 0
        set(value) {
            field = if (value >= Integer.MAX_VALUE) 1 else value
        }

    /**
     *  作用同[Activity.startActivity]
     *  示例：
     *  <pre>
     *      //不携带参数
     *      ActivityMessenger.startActivity<TestActivity>(this)
     *
     *      //携带参数（可连续多个键值对）
     *      ActivityMessenger.startActivity<TestActivity>(this, "Key" to "Value")
     *  </pre>
     *
     * @param TARGET 要启动的Activity
     * @param starter 发起的Activity
     * @param params extras键值对
     */
    inline fun <reified TARGET: Activity> startActivity(
        starter: FragmentActivity,
        vararg params: Pair<String, Any>
    ) = starter.startActivity(Intent(starter, TARGET::class.java).putExtras(*params))

}