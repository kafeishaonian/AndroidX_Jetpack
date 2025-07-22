package com.example.module_fundamental.thread_task

import io.reactivex.Scheduler

interface PostExecutionThread {
    fun getScheduler(): Scheduler
}