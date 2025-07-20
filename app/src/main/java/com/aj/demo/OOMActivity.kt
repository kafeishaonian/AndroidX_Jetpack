package com.aj.demo

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class OOMActivity: AppCompatActivity() {

    companion object{
        private var sLeakedActivity: Activity? = null
        private var sHelper: LeakyHelper? = null
    }

    inner class LeakyHelper {
        fun doAction() {
            // 访问 Activity 的成员变量
            findViewById<TextView>(R.id.main_text)
        }
    }

    // ✅ 未清理的 Handler
    private val leakyHandler = Handler(Looper.getMainLooper())
    private val leakyRunnable = Runnable {
        // 即使 Activity 销毁后仍执行操作
        Toast.makeText(this@OOMActivity, "Still leaking!", Toast.LENGTH_SHORT).show()
    }
    object BadSingleton {
        private var leakyContext: Context? = null

        fun initialize(context: Context) {
            if (leakyContext == null) {
                leakyContext = context // 保存 Activity 引用
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_oom)
        sLeakedActivity = this

        // 启动延迟消息（60秒后执行）
        leakyHandler.postDelayed(leakyRunnable, 60000)

        // 初始化错误单例
        BadSingleton.initialize(this)

        // 创建内部类实例（增强泄漏）
        LeakyHelper().doAction()

        // 添加未取消注册的监听器
        findViewById<TextView>(R.id.main_text).setOnClickListener {
            Toast.makeText(this, "Button clicked", Toast.LENGTH_SHORT).show()
        }

    }

}