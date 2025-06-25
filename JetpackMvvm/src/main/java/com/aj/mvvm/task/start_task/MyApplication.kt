package com.aj.mvvm.task.start_task

import android.app.Application

class MyApplication : Application(){

    override fun onCreate() {
        super.onCreate()

        val initTasks = taskGraph {
            task("db_init") {

            }

            task("network_init", dependsOn = listOf("db_init")) {
                // 网络库初始化
            }

            task("user_prefetch", dependsOn = listOf("db_init", "network_init")) {
                // 用户数据预加载
            }
        }


        val monitor = TaskMonitor()
        val monitoredTasks = initTasks.map { monitor.wrapTask(it) } + monitor
        TaskExecutor(monitoredTasks).execute()
    }

}