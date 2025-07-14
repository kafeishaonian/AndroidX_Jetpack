package com.example.module_fundamental.dialogTask

import android.app.Activity
import android.app.Dialog
import java.lang.ref.WeakReference

class Task : Comparable<Task> {

    companion object {
        const val TYPE_DIALOG = 0
        const val TYPE_ACTIVITY = 1
    }

    var taskId: String = ""
    var priority: Int = 5
    var type: Int = TYPE_DIALOG
    var delayMillis: Long = 3000
    var executor: Runnable? = null
    var startTime: Long = 0
    var minShowDuration: Long = 2000
    var activityRef: WeakReference<Activity>? = null // 绑定的Activity弱引用
    var dialogRef: WeakReference<Dialog>? = null     // 绑定的Dialog弱引用


    constructor()

    constructor(config: TaskConfig) {
        taskId = config.taskId
        priority = config.priority
        type = config.type
        delayMillis = config.delayMillis
        minShowDuration = config.minShowDuration
    }

    override fun compareTo(other: Task): Int {
        return other.priority - priority
    }

    fun getActivity(): Activity? = activityRef?.get()

    fun setActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun getDialog(): Dialog? = dialogRef?.get()

    fun setDialog(dialog: Dialog) {
        dialogRef = WeakReference(dialog)
    }
}