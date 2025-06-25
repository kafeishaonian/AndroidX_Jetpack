package com.aj.mvvm.task.start_task

import android.os.Handler
import android.os.Looper
import android.os.Process
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

class TaskExecutor(private val tasks: List<ITask>) {

    private val cpuExecutor: ExecutorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors(),
        PriorityThreadFactory("task-pool-")
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private val dependencyGraph = TaskDependencyGraph()
    private lateinit var completionLatch: CountDownLatch


    private inner class PriorityThreadFactory(
        private val prefix: String
    ): ThreadFactory {
        override fun newThread(runnable: Runnable?): Thread {
            return Thread{
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
                runnable?.run()
            }.apply {
                name = "$prefix${nextThreadId()}"
            }
        }

        private var threadId = 0
        private fun nextThreadId() : Int = synchronized(this) {threadId ++ }
    }

    // 执行所有任务
    fun execute(timeoutMillis: Long = 3000) {
        //构建依赖图
        tasks.forEach { dependencyGraph.addTask(it) }

        //拓扑排序
        val sortedTaskIds = dependencyGraph.topologicalSort()

        //初始化完成计数器
        completionLatch = CountDownLatch(sortedTaskIds.size)

        //按排序顺序执行任务
        sortedTaskIds.forEach { taskId ->
            dependencyGraph.getAllTasks()[taskId]?.let { task ->
                if (task.runOnMainThread()) {
                    executeOnMainThread(task)
                } else {
                    executeOnBackground(task)
                }
            }
        }

        //等待所有任务完成
        completionLatch.await(timeoutMillis, TimeUnit.MILLISECONDS)
    }

    private fun executeOnMainThread(task: ITask) {
        mainHandler.post {
            try {
                task.run()
            } finally {
                completionLatch.countDown()
            }
        }
    }

    private fun executeOnBackground(task: ITask) {
        cpuExecutor.execute {
            Process.setThreadPriority(task.priority())

            try {
                task.run()
            } finally {
                completionLatch.countDown()
            }
        }
    }
}