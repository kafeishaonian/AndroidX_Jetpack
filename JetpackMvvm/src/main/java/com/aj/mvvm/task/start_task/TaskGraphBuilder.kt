package com.aj.mvvm.task.start_task

class TaskGraphBuilder {

    private val tasks = mutableListOf<ITask>()

    fun task(
        id: String,
        dependsOn: List<String> = emptyList(),
        uiThread: Boolean = false,
        action: () -> Unit
    ) {
        tasks.add(object : ITask {
            override val taskId: String = id
            override fun dependsOn(): List<String> = dependsOn
            override fun runOnMainThread(): Boolean = uiThread
            override fun run() = action()
        })
    }

    fun build(): List<ITask> = tasks
}


// 使用扩展函数简化任务配置
fun taskGraph(block: TaskGraphBuilder.() -> Unit): List<ITask> {
    return TaskGraphBuilder().apply(block).build()
}