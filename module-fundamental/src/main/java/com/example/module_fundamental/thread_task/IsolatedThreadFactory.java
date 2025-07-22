package com.example.module_fundamental.thread_task;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class IsolatedThreadFactory implements ThreadFactory {

    private static final AtomicInteger count = new AtomicInteger(1);

    @Override
    public Thread newThread(Runnable r) {
        IsolatedThread thread = new IsolatedThread(r, "Isolated #" + count.getAndIncrement());
        thread.setPriority(10);
        return thread;
    }
}
