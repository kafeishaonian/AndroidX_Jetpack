package com.example.module_fundamental.cement2

import android.os.Handler
import java.util.concurrent.Executor

/**
 * Handler执行器
 * 将Executor的任务提交到Handler执行
 */
class HandlerExecutor(private val handler: Handler) : Executor {
    
    override fun execute(command: Runnable?) {
        command?.let {
            handler.post(it)
        }
    }
}