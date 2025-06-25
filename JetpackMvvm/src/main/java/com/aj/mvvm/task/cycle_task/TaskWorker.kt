package com.aj.mvvm.task.cycle_task

/**
 * 任务工作者接口
 */
interface TaskWorker {
    /**
     * 检查任务是否应继续执行
     */
    fun isTaskActive(): Boolean

    /**
     * 执行单个任务单元
     */
    suspend fun processTask(): TaskResult

    /**
     * 任务开始前回调
     */
    fun onTaskStart() {}

    /**
     * 任务结束时回调
     */
    fun onTaskComplete(result: TaskResult){}
}