package com.aj.mvvm.task.start_task

import android.os.Process

interface ITask {

    /**
     * 任务依赖的唯一标识
     */
    fun dependsOn(): List<String>

    /**
     * 执行任务
     */
    fun run()

    fun runOnMainThread(): Boolean = false

    fun priority(): Int = Process.THREAD_PRIORITY_BACKGROUND

    val taskId: String

}