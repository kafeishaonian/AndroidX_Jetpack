package com.example.module_fundamental.thread_task;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ThreadPoolExecutor extends ScheduledThreadPoolExecutor {
    private String name;

    public ThreadPoolExecutor(String name, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, threadFactory, handler);
        setMaximumPoolSize(maximumPoolSize);
        setKeepAliveTime(keepAliveTime, unit);
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
