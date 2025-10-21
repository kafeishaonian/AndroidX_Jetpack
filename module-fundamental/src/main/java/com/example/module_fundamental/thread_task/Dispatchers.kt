package com.example.module_fundamental.thread_task

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainCoroutineDispatcher

/**
 * 线程调度器统一管理类（单例模式）
 */
object Dispatchers {
    /**
     * 主线程调度器的延迟初始化
     */
    private val Main: Lazy<MainCoroutineDispatcher> = lazy {
        Dispatchers.Main
    }


    /** 主线程调度器 */
    fun getMain() : MainCoroutineDispatcher{
        return Main.value
    }

    /** 用户相关操作调度器 */
    fun getUser(): CoroutineDispatcher {
        return Executors.INSTANCE.getUserExecutorInfo().getDispatcher()
    }


    /** 内部任务调度器 */
    fun getInner(): CoroutineDispatcher {
        return Executors.INSTANCE.getInnerExecutorInfo().getDispatcher()
    }

    /** 本地任务调度器 */
    fun getLocal(): CoroutineDispatcher {
        return Executors.INSTANCE.getLocalExecutorInfo().getDispatcher()
    }

    /** 统计任务调度器 */
    fun getStatistics(): CoroutineDispatcher {
        return Executors.INSTANCE.getStatisticsExecutorInfo().getDispatcher()
    }

    /** 独立线程池调度器 */
    fun getIsolated(): CoroutineDispatcher {
        return Executors.INSTANCE.getIsolatedExecutorInfo().getDispatcher()
    }
}






