package com.example.module_fundamental.dialogTask

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

abstract class BaseTaskActivity: AppCompatActivity() {

    protected var boundTask: Task? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val taskId = intent.getStringExtra("TASK_ID")
        taskId?.let {
            createAndBindTask(it)
        }
    }

    fun createAndBindTask(taskId: String) {
        boundTask = Task()
        boundTask?.taskId = taskId
        boundTask?.startTime = System.currentTimeMillis()
        boundTask?.setActivity(this)


        // 绑定到任务管理器
        val current: Task = TaskManager.get().getTask()
        if (taskId.equals(current.taskId)) {
            current.setActivity(this)
        }
    }

    override fun onResume() {
        super.onResume()
        PageTracker.get().onActivityResumed(this)
    }

    override fun onPause() {
        super.onPause()
        PageTracker.get().onActivityPaused(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 通知任务完成
        if (boundTask != null && TaskManager.get().getTask() != null &&
            boundTask!!.taskId == TaskManager.get().getTask().taskId
        ) {
            // 检查是否满足最小显示时间要求

            val elapsed = System.currentTimeMillis() - boundTask!!.startTime
            val config: TaskConfig? = TaskFactory.getConfigCache()[boundTask?.taskId]
            val minDuration = config?.minShowDuration ?: 2000

            if (elapsed < minDuration) {
                val remaining = minDuration - elapsed
                TaskManager.get()
                    .onTaskFinished(boundTask!!, remaining + boundTask!!.delayMillis)
            } else {
                TaskManager.get().onTaskFinished(boundTask!!)
            }
        }
    }

    fun setCurrentFragment(fragment: Fragment?) {
        PageTracker.get().setCurrentFragment(fragment!!)
    }

}