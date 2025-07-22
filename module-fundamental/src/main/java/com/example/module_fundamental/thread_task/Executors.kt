package com.example.module_fundamental.thread_task

import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService

class Executors private constructor(){

    companion object {
        val INSTANCE: Executors = Executors()
    }

    private val userExecutorInfo: Lazy<ExecutorInfo> = lazy {
        ExecutorInfo("TYPE_RIGHT_NOW", ThreadUtils.TYPE_RIGHT_NOW)
    }

    private val innerExecutorInfo: Lazy<ExecutorInfo> = lazy {
        ExecutorInfo("TYPE_INNER", ThreadUtils.TYPE_INNER)
    }

    private val localExecutorInfo: Lazy<ExecutorInfo> = lazy {
        ExecutorInfo("TYPE_RIGHT_NOW_LOCAL", ThreadUtils.TYPE_RIGHT_NOW_LOCAL)
    }

    private val statisticsExecutorInfo: Lazy<ExecutorInfo> = lazy {
        ExecutorInfo("TYPE_STATISTICS", ThreadUtils.TYPE_STATISTICS)
    }

    private val isolatedExecutorInfo: Lazy<ExecutorInfo> = lazy {
        ExecutorInfo("TYPE_ISOLATED", ThreadUtils.TYPE_ISOLATED)
    }

    fun getUserExecutorInfo(): ExecutorInfo = userExecutorInfo.value

    fun getInnerExecutorInfo(): ExecutorInfo = innerExecutorInfo.value

    fun getLocalExecutorInfo(): ExecutorInfo = localExecutorInfo.value

    fun getStatisticsExecutorInfo(): ExecutorInfo = statisticsExecutorInfo.value

    fun getIsolatedExecutorInfo(): ExecutorInfo = isolatedExecutorInfo.value


    class ExecutorInfo(val executorName: String, val executorType: Int) {
        private var mDispatcher: CoroutineDispatcher? = null
        private var mExecutor: Executor? = null

        private var threadExecutor: Lazy<ScheduledThreadExecutor> = lazy {
            ScheduledThreadExecutor(executorType)
        }

        private fun getThreadExecutor(): ThreadExecutor{
            return threadExecutor.value
        }

        fun getScheduler(): Scheduler {
            return Schedulers.from(getThreadExecutor())
        }


        @Synchronized
        fun getDispatcher(): CoroutineDispatcher {
            val executor = ExampleThreadUtils.getRealExecutor(executorType)
            if (mExecutor != executor) {
                mExecutor = executor
                mDispatcher = (SafeExecutor(executor) as ExecutorService).asCoroutineDispatcher()
            }
            return mDispatcher ?: throw IllegalStateException("Executor=$executorName init failed")
        }
    }
}
