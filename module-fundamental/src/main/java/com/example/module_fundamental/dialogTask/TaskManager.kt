package com.example.module_fundamental.dialogTask

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import java.util.PriorityQueue

class TaskManager private constructor() : PageTracker.PageConditionObserver {

    companion object {
        @Volatile
        private var instance: TaskManager? = null

        fun init() {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = TaskManager()
                        // 注册页面观察器
                        PageTracker.get().addPageObserver(instance!!)
                    }
                }
            }
        }

        fun get(): TaskManager = instance ?: throw IllegalStateException("TaskManager not initialized")
    }

    private val MIN_TASK_INTERVAL = 300   // 最小任务间隔300ms
    private val TASK_TIMEOUT = 30 * 1000L // 30秒超时

    private val pendingQueue = PriorityQueue<Task>()
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile var currentTask: Task? = null
        private set
    private var isCurrentTaskActive = false
    private var taskCompletionTime: Long = 0

    private val minDurationPassedMap = HashMap<String, Boolean>()
    private val delayedCompletions = ArrayList<Runnable>()
    private val pageTracker = PageTracker.get()

    /**
     * 添加新任务（应用内调用）
     */
    @Synchronized
    fun addTask(task: Task) {
        // 检查是否满足页面条件
        val config = TaskFactory.getConfigCache()[task.taskId]
        if (config != null && config.requiredPage != null) {
            // 如果当前已在所需页面，直接执行
            if (!pageTracker.isInPage(config.requiredPage!!)) {
                // 页面条件不满足，等待
                Log.d("TaskManager", "Task ${task.taskId} requires page: ${config.requiredPage!!.simpleName}")
                return
            }
        }
        pendingQueue.offer(task)
        tryScheduleNext()
    }

    /**
     * 添加新任务（工厂方法）
     */
    @Synchronized
    fun addTask(taskId: String, context: Context) {
        TaskFactory.createTask(taskId, context)?.let { task ->
            addTask(task)
        }
    }

    /**
     * 尝试调度下一个任务
     */
    @Synchronized
    private fun tryScheduleNext() {
        if (isCurrentTaskActive || pendingQueue.isEmpty()) return

        // 确保有足够的时间间隔
        val currentTime = System.currentTimeMillis()
        if (currentTime - taskCompletionTime < MIN_TASK_INTERVAL) {
            mainHandler.postDelayed(this::tryScheduleNext,
                MIN_TASK_INTERVAL - (currentTime - taskCompletionTime))
            return
        }

        val nextTask = pendingQueue.poll() ?: return

        currentTask = nextTask
        isCurrentTaskActive = true
        minDurationPassedMap[nextTask.taskId] = false

        // 设置任务开始时间
        nextTask.startTime = currentTime

        // 启动超时保护
        startTaskTimeout(nextTask)

        // 启动最小展示时长计时器
        startMinDurationTimer(nextTask)

        // 执行任务
        mainHandler.post { executeTask(nextTask) }
    }

    private fun executeTask(task: Task) {
        val config = TaskFactory.configCache[task.taskId]
        if (config != null && config.requiredPage != null) {
            // 如果当前已在所需页面，直接执行
            if (pageTracker.isInPage(config.requiredPage!!)) {
                runTaskExecutor(task)
                return
            }

            // 否则等待页面条件满足
            Log.d("TaskManager", "Waiting for page: ${config.requiredPage!!.simpleName}")
            return
        }

        // 普通任务直接执行
        runTaskExecutor(task)
    }

    private fun runTaskExecutor(task: Task) {
        try {
            task.executor?.run()
        } catch (e: Exception) {
            Log.e("TaskManager", "Failed to execute task: ${task.taskId}", e)
            onTaskFinished(task)
        }
    }
    /**
     * 启动任务超时保护
     */
    private fun startTaskTimeout(task: Task) {
        mainHandler.postDelayed({
            if (isCurrentTaskActive && currentTask != null &&
                task.taskId == currentTask!!.taskId) {
                Log.w("TaskManager", "Task timeout: ${task.taskId}")
                forceFinishCurrentTask()
                tryScheduleNext()
            }
        }, TASK_TIMEOUT)
    }

    /**
     * 启动最小展示时长计时器
     */
    private fun startMinDurationTimer(task: Task) {
        val minDuration = TaskFactory.configCache[task.taskId]?.minShowDuration ?: 2000

        mainHandler.postDelayed({
            minDurationPassedMap[task.taskId] = true
            checkTaskCompletionDelay(task)
        }, minDuration)
    }
    /**
     * 当页面满足条件时的回调
     */
    override fun check(activity: Activity?, fragment: Fragment?) {
        if (!isCurrentTaskActive || currentTask == null) return

        val config = TaskFactory.configCache[currentTask!!.taskId]
        config?.requiredPage?.takeIf {
            pageTracker.isInPage(it)
        }?.let {
            Log.d("TaskManager", "Page condition satisfied for task: ${currentTask!!.taskId}")
            runTaskExecutor(currentTask!!)
        }
    }

    override fun isSatisfied(): Boolean = false

    /**
     * 强制结束当前任务
     */
    private fun forceFinishCurrentTask() {
        if (isCurrentTaskActive && currentTask != null) {
            val task = currentTask!!
            isCurrentTaskActive = false
            currentTask = null

            // 关闭正在显示的UI
            task.getDialog()?.let { safeDismissDialog(it) }

            // 重新加入队列
            pendingQueue.offer(task)

            Log.i("TaskManager", "Task force finished: ${task.taskId}")
        }
    }

    /**
     * 任务完成时调用（由UI组件触发）
     */
    fun onTaskFinished(task: Task) = onTaskFinished(task, task.delayMillis)

    fun onTaskFinished(task: Task, customDelay: Long) {
        if (!isCurrentTaskActive || currentTask == null ||
            task.taskId != currentTask!!.taskId) {
            return
        }

        // 检查是否满足最小展示时间
        val minDurationPassed = minDurationPassedMap[task.taskId] ?: false

        if (!minDurationPassed) {
            Log.d("TaskManager", "Task finished before min duration, delaying: ${task.taskId}")
            // 计算剩余等待时间
            val elapsed = System.currentTimeMillis() - task.startTime
            val remaining = task.minShowDuration - elapsed
            if (remaining > 0) {
                // 延迟完成任务
                val delayedComplete = Runnable { onTaskFinished(task, customDelay) }
                mainHandler.postDelayed(delayedComplete, remaining)
                delayedCompletions.add(delayedComplete)
                return
            }
        }

        // 正式完成任务
        Log.i("TaskManager", "Task completed: ${task.taskId}")
        isCurrentTaskActive = false
        taskCompletionTime = System.currentTimeMillis()
        currentTask = null

        // 执行延迟时间
        mainHandler.postDelayed({
            synchronized(this) {
                tryScheduleNext()
            }
        }, customDelay)
    }

    /**
     * 清理资源
     */
    @Synchronized
    fun clear() {
        pendingQueue.clear()
        minDurationPassedMap.clear()
        for (completion in delayedCompletions) {
            mainHandler.removeCallbacks(completion)
        }
        delayedCompletions.clear()
        if (isCurrentTaskActive && currentTask != null) {
            currentTask!!.getDialog()?.let { safeDismissDialog(it) }
        }
        isCurrentTaskActive = false
        currentTask = null
    }

    private fun safeDismissDialog(dialog: Dialog) {
        if (dialog.isShowing) {
            try {
                dialog.dismiss()
            } catch (e: Exception) {
                Log.e("TaskManager", "Error dismissing dialog", e)
            }
        }
    }

    fun getCurrentTask(): Task {
        return currentTask!!
    }

    // 在 TaskManager 类中添加以下方法
    private fun checkTaskCompletionDelay(task: Task) {
        // 检查任务是否已经完成但延迟尚未处理
        if (!isCurrentTaskActive || currentTask != task) return

        // 检查最小展示时间是否已满足
        val minDurationPassed = minDurationPassedMap[task.taskId] ?: false

        if (minDurationPassed) {
            // 如果最小展示时间已满足，但任务还未自然完成
            // 可能是用户长时间未关闭，需要强制处理
            Log.d("TaskManager", "Min duration passed but task not finished: ${task.taskId}")

            // 获取实际延迟时间（配置延迟 + 额外等待时间）
            val config = TaskFactory.configCache[task.taskId]
            val delay = config?.delayMillis ?: task.delayMillis

            // 处理任务完成
            onTaskFinished(task, delay)
        }
    }
}