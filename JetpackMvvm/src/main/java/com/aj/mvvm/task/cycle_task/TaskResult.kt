package com.aj.mvvm.task.cycle_task

sealed class TaskResult {
    object Success: TaskResult()
    data class Retry<T>(val t: T?): TaskResult()
    data class Delay(val duration: Long): TaskResult()
    data class Failure(val error: Throwable): TaskResult()
}