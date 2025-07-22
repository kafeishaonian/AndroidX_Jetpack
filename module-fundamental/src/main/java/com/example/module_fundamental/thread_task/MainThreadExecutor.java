package com.example.module_fundamental.thread_task;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class MainThreadExecutor {

    private static volatile Handler handler;

    public static void post(Object tag, Runnable runnable) {
        if ((tag instanceof Number) || (tag instanceof CharSequence)) {
            tag = tag.toString().intern();
        }
        Message message = Message.obtain(getHandler(), runnable);
        message.obj = tag;
        getHandler().sendMessage(message);
    }


    public static void postDelayed(Object tag, Runnable runnable, long delayMill) {
        if (tag == null) {
            throw new IllegalArgumentException("tag is null");
        }

        if(runnable == null) {
            throw new IllegalArgumentException("runnable is null");
        }

        if (delayMill <= 0) {
            throw new IllegalArgumentException("delayMill <= 0");
        }

        if ((tag instanceof Number) || (tag instanceof CharSequence)) {
            tag = tag.toString().intern();
        }

        Message message = Message.obtain(getHandler(), runnable);
        message.obj = tag;
        getHandler().sendMessageDelayed(message, delayMill);
    }

    public static void cancelSpecificRunnable(Object tag, Runnable runnable) {
        if (tag == null) {
            throw new IllegalArgumentException("tag is null");
        }
        if(runnable == null) {
            throw new IllegalArgumentException("runnable is null");
        }

        if((tag instanceof Number) || (tag instanceof CharSequence)) {
            tag = tag.toString().intern();
        }

        getHandler().removeCallbacks(runnable, tag);
    }

    public static void cancelAllRunables(Object tag) {
        if(tag == null) {
            throw new IllegalArgumentException("tag is null");
        }

        if((tag instanceof Number) || (tag instanceof CharSequence)) {
            tag = tag.toString().intern();
        }

        getHandler().removeCallbacksAndMessages(tag);
    }

    private static Handler getHandler() {
        if (handler == null) {
            synchronized (MainThreadExecutor.class) {
                if(handler == null) {
                    handler = new Handler(Looper.getMainLooper());
                }
            }
        }
        return handler;
    }



}
