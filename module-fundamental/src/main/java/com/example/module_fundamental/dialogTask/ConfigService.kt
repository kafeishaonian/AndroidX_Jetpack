package com.example.module_fundamental.dialogTask


object ConfigService {

    fun updateTaskConfigs(newConfigs: List<TaskConfig>) {
        val factory = TaskFactory

        // 第一步：注册新配置
        for (config in newConfigs) {
            factory.registerOrUpdateConfig(config)
        }


        // 第二步：清理不再存在的配置
        val toRemove: MutableList<String> = ArrayList()
        for (taskId in factory.configCache.keys) {
            var found = false
            for (config in newConfigs) {
                if (config.taskId == taskId) {
                    found = true
                    break
                }
            }
            if (!found) {
                toRemove.add(taskId)
            }
        }

        for (taskId in toRemove) {
            factory.removeConfig(taskId)
        }
    }

}