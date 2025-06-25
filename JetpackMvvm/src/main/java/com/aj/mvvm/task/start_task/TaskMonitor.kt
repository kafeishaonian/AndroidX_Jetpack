package com.aj.mvvm.task.start_task

/**
 * 任务状态监控
 */
class TaskMonitor: ITask {

    override val taskId: String = "task_monitor"

    override fun dependsOn(): List<String> = emptyList()

    override fun runOnMainThread(): Boolean = true

    private val taskMetrics = mutableMapOf<String, Pair<Long, Long>>()


    fun wrapTask(task: ITask): ITask = object: ITask by task {
        override fun run() {
            val start = System.currentTimeMillis()
            task.run()
            val end = System.currentTimeMillis()
            synchronized(taskMetrics) {
                taskMetrics[task.taskId] = start to (end - start)
            }
        }
    }

    override fun run() {
        val report = taskMetrics.entries.joinToString("\n") {
            "${it.key}: ${it.value.second}ms"
        }
        println("===== TASK EXECUTION REPORT =====\n$report")
    }
}