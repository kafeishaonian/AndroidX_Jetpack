package com.example.module_fundamental.thread_task

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers

object ThreadExecutors {

    private val Main: Lazy<PostExecutionThread> = lazy {
        object: PostExecutionThread{
            override fun getScheduler(): Scheduler {
                return AndroidSchedulers.mainThread()
            }
        }
    }


    fun getMain(): Scheduler {
        return Main.value.getScheduler()
    }

    fun getUser(): Scheduler {
        return Executors.INSTANCE.getUserExecutorInfo().getScheduler()
    }

    fun getInner(): Scheduler {
        return Executors.INSTANCE.getInnerExecutorInfo().getScheduler()
    }

    fun getLocal(): Scheduler {
        return Executors.INSTANCE.getLocalExecutorInfo().getScheduler()
    }

    fun getStatistics(): Scheduler {
        return Executors.INSTANCE.getStatisticsExecutorInfo().getScheduler()
    }

    fun getIsolated(): Scheduler {
        return Executors.INSTANCE.getIsolatedExecutorInfo().getScheduler()
    }


}
