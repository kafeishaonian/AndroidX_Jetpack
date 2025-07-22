package com.example.module_fundamental.thread_task

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object ExampleThreadUtils {

    private val ISOLATED_EXECUTOR: Lazy<ThreadPoolExecutor> = lazy {
        val executor = ThreadPoolExecutor(
            "Isolated",
            0,
            2,
            10L,
            TimeUnit.SECONDS,
            LinkedBlockingQueue(),
            IsolatedThreadFactory()
        ) { _, _ -> }
        executor.allowCoreThreadTimeOut(true)
        executor
    }

    private fun getIsolatedExecutor(): ScheduledExecutorService = ISOLATED_EXECUTOR.value

    fun getRealExecutor(executorType: Int): ScheduledExecutorService {
        return if (executorType == ThreadUtils.TYPE_ISOLATED) {
            getIsolatedExecutor()
        } else {
            ThreadUtils.getExecutorInfo(executorType)
        }
    }

    fun whenNeedClean() {
        ThreadUtils.shutDown(2)
    }

    fun whenSwitchAccount() {
        ThreadUtils.shutDownAll()
    }
}

