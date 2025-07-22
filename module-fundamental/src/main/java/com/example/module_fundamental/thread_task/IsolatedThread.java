package com.example.module_fundamental.thread_task;

public class IsolatedThread extends Thread {

    public IsolatedThread(Runnable runnable, String threadName) {
        super(runnable, threadName);
    }

}
