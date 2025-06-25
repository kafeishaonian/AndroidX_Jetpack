package com.aj.mvvm.task.start_task

import java.util.LinkedList

class TaskDependencyGraph {

    private val tasks = mutableMapOf<String, ITask>()
    private val dependencies = mutableMapOf<String, List<String>>()
    private val reverseDependencies = mutableMapOf<String, MutableList<String>>()


    fun addTask(task: ITask) {
        if (task.taskId in tasks) return

        tasks[task.taskId] = task
        dependencies[task.taskId] = task.dependsOn()

        // 构建反向依赖关系
        task.dependsOn().forEach { depId ->
            reverseDependencies.getOrPut(depId) { mutableListOf() }.add(task.taskId)
        }
    }

    // 获取所有任务
    fun getAllTasks(): Map<String, ITask> = tasks


    //拓扑排序 (BFS Kahn算法)
    fun topologicalSort(): List<String> {
        // 初始化入度计数
        val inDegree = mutableMapOf<String, Int>()
        val queue = ArrayDeque<String>()

        tasks.keys.forEach { taskId ->
            inDegree[taskId] = dependencies[taskId]?.size ?: 0
            if (inDegree[taskId] == 0) queue.addLast(taskId)
        }

        // Kahn算法拓扑排序
        val sortedList = mutableListOf<String>()

        while (!queue.isEmpty()) {
            val taskId = queue.removeFirst()
            sortedList.add(taskId)

            reverseDependencies[taskId]?.forEach { childId ->
                inDegree[childId] = inDegree[childId]?.minus(1) ?: 0
                if (inDegree[childId] == 0) queue.addLast(childId)
            }
        }

        //检查循环依赖
        if (sortedList.size != tasks.size) {
            val circularTasks = tasks.keys - sortedList.toSet()
            throw IllegalStateException("检测到循环依赖: ${circularTasks.joinToString()}")
        }

        return sortedList
    }

}