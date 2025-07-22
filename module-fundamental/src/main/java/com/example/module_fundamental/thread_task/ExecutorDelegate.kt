package com.example.module_fundamental.thread_task

import java.util.concurrent.ScheduledExecutorService

class ExecutorDelegate(private val executorType: Int) {

    fun getValue(): ScheduledExecutorService {
        return ExampleThreadUtils.getRealExecutor(executorType)
    }
}
