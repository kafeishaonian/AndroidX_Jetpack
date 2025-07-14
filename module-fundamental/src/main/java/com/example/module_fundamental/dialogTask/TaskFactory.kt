package com.example.module_fundamental.dialogTask

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.module_fundamental.dialogTask.demo.ProfileActivity

object TaskFactory {
    internal val configCache = HashMap<String, TaskConfig>()
    private val taskShowCount = HashMap<String, Int>()

    fun getConfigCache() = configCache

    /**
     * 注册或更新任务配置
     */
    fun registerOrUpdateConfig(config: TaskConfig) {
        configCache[config.taskId] = config
        taskShowCount.putIfAbsent(config.taskId, 0)
    }

    /**
     * 移除任务配置
     */
    fun removeConfig(taskId: String) {
        configCache.remove(taskId)
        taskShowCount.remove(taskId)
    }

    /**
     * 创建任务实例
     */
    fun createTask(taskId: String, context: Context): Task? {
        val config = configCache[taskId] ?: run {
            Log.w("TaskFactory", "No config found for task: $taskId")
            return null
        }

        // 检查任务展示次数限制
        val count = taskShowCount[taskId]
        if (count != null && count >= config.maxShowCount) {
            Log.i("TaskFactory", "Task $taskId reached max show count")
            return null
        }

        val task = Task(config)

        // 根据任务类型创建执行器
        task.executor = when(config.type) {
            Task.TYPE_DIALOG -> createDialogExecutor(taskId, context, task)
            Task.TYPE_ACTIVITY -> createActivityExecutor(taskId, context, task)
            else -> {
                Log.w("TaskFactory", "Unsupported task type: ${config.type}")
                return null
            }
        }
        return task
    }

    /**
     * 记录任务展示次数
     */
    fun recordTaskShown(taskId: String) {
        taskShowCount[taskId]?.let { count ->
            taskShowCount[taskId] = count + 1
        }
    }

    private fun createDialogExecutor(taskId: String, context: Context, task: Task) = Runnable {
        val config = configCache[taskId] ?: return@Runnable

        val dialog = createDialogForTask(taskId, context, config)
        if (dialog == null) return@Runnable

        // 设置绑定
        task.setDialog(dialog)

        // 安全显示对话框
        safeShowDialog(dialog, context)

        // 记录展示次数
        recordTaskShown(taskId)
    }

    private fun createActivityExecutor(taskId: String, context: Context, task: Task) = Runnable {
        val config = configCache[taskId] ?: return@Runnable

        // 创建意图启动Activity
        val intent = createIntentForTask(taskId, context, config)
        if (intent == null) return@Runnable

        // 设置绑定（稍后在Activity的onCreate中绑定）

        // 启动Activity
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)

            // 记录展示次数
            recordTaskShown(taskId)
        } catch (e: Exception) {
            Log.e("TaskFactory", "Failed to start activity", e)
        }
    }

    private fun createDialogForTask(taskId: String, context: Context, config: TaskConfig): Dialog? {
        // 实际应用中应根据taskId创建对应的Dialog
        // 这里用通用Dialog代替
        return Dialog(context).apply {
            // 设置布局等操作
//            setContentView(R.layout.default_dialog)
//            setCancelable(true)
//
//            // 设置关闭监听
//            setOnDismissListener {
//                val currentTask = TaskManager.instance?.currentTask
//                if (currentTask != null && taskId == currentTask.taskId) {
//                    TaskManager.instance?.onTaskFinished(currentTask)
//                }
//            }
        }
    }

    private fun createIntentForTask(taskId: String, context: Context, config: TaskConfig): Intent {
        // 实际应用中应根据taskId启动对应的Activity
        // 这里用通用Activity代替
        return Intent(context, ProfileActivity::class.java).apply {
            putExtra("TASK_ID", taskId)

            // 传递额外参数
            for ((key, value) in config.extra) {
                when (value) {
                    is String -> putExtra(key, value)
                    is Int -> putExtra(key, value)
                    is Boolean -> putExtra(key, value)
                    // 可以扩展其他类型
                }
            }
        }
    }

    private fun safeShowDialog(dialog: Dialog, context: Context) {
        try {
            if (!isContextValid(context)) {
                Log.w("TaskFactory", "Invalid context for dialog")
                return
            }

            if (!dialog.isShowing) {
                dialog.show()
            }
        } catch (e: Exception) {
            Log.e("TaskFactory", "Failed to show dialog", e)
        }
    }

    private fun isContextValid(context: Context): Boolean {
        if (context is Activity) {
            return !context.isFinishing && !context.isDestroyed
        }
        return true
    }

}