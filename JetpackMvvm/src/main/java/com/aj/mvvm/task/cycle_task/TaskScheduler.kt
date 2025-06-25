package com.aj.mvvm.task.cycle_task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * 通用任务调取器
 */
class TaskScheduler(
    private val scope: CoroutineScope,
    private val worker: TaskWorker,
    private val backoffStrategy: BackoffStrategy = ExponentialBackoff(),
    private val config: TaskConfig = TaskConfig(),
) {

    private val jobTracker = JobTracker()
    private val taskListeners = mutableListOf<(TaskResult) -> Unit>()

    fun launchTask() {
        if (jobTracker.isActive() || !worker.isTaskActive()) return

        jobTracker.launch {
            worker.onTaskStart()
            runTaskCycle()
            worker.onTaskComplete(getFinalResult())
        }
    }

    fun onTaskComplete(listener: (TaskResult) -> Unit) {
        taskListeners.add(listener)
    }


    private suspend fun runTaskCycle() {
        while (worker.isTaskActive() && coroutineContext.isActive) {
            val result = runCatching {
                worker.processTask()
            }.getOrElse { e->
                if (config.retryOnException) {
                    TaskResult.Retry(null)
                } else {
                    TaskResult.Failure(e)
                }
            }
            handleResult(result)
        }
    }

    private suspend fun handleResult(result: TaskResult) {
        when (result) {
            is TaskResult.Success -> {
                backoffStrategy.recordSuccess()
                notifyListeners(result)
            }
            is TaskResult.Retry<*> -> {
                val afterDelay = backoffStrategy.recordFailure()
                notifyListeners(result)
                delay(afterDelay)
            }
            is TaskResult.Delay -> {
                notifyListeners(result)
                delay(result.duration)
            }
            is TaskResult.Failure -> {
                notifyListeners(result)
            }
        }
    }


    private fun notifyListeners(result: TaskResult) {
        taskListeners.forEach { it(result) }
    }


    private fun getFinalResult(): TaskResult {
        return if (!worker.isTaskActive()) {
            TaskResult.Failure(IllegalStateException("Task manually stopped"))
        } else {
            TaskResult.Failure(IllegalStateException("Task completed without final state"))
        }
    }

    data class TaskConfig(
        val minDelay: Long = 0,
        val maxDelay: Long = Long.MAX_VALUE,
        val maxRetries: Int = Int.MAX_VALUE,
        val retryOnException: Boolean = true
    )


    private inner class JobTracker {
        private var currentJob: Job? = null

        fun isActive(): Boolean {
            return currentJob?.isActive ?: false
        }

        fun launch(block: suspend CoroutineScope.() -> Unit) {
            currentJob = scope.launch(block = block)
        }
    }
}