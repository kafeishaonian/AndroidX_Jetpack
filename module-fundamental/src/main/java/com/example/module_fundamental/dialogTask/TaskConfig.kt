package com.example.module_fundamental.dialogTask

class TaskConfig {
    var taskId: String = ""           // 任务唯一标识
    var priority: Int = 5             // 优先级 (0-10, 默认5)
    var type: Int = -1                // 类型 (Dialog/Activity)
    var delayMillis: Long = 3000      // 默认延迟时间 3秒
    var requiredPage: Class<*>? = null// 所需页面类
    var minShowDuration: Long = 2000  // 最小显示时长
    var requiresLogin: Boolean = false // 需要登录
    var maxShowCount: Int = 1         // 最大展示次数
    val extra: MutableMap<String, Any> = HashMap() // 额外参数
}