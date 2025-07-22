package com.example.module_fundamental.thread_task;

import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池工具类
 */
public class ThreadUtils {
    private static final long DEFAULT_KEEP_ALIVE_TIME = 60;
    private static final int INNER_THREAD_SIZE = 4;
    private static final int RIGHT_NOW_LOCAL_THREAD_SIZE = 5;
    private static final int RIGHT_NOW_THREAD_SIZE = 10;
    private static final int STATISTICS_THREAD_SIZE = 2;

    // 线程池类型标识
    public static final int TYPE_ISOLATED = -1;
    public static final int TYPE_INNER = 1;
    public static final int TYPE_RIGHT_NOW = 2;
    public static final int TYPE_RIGHT_NOW_LOCAL = 3;
    public static final int TYPE_STATISTICS = 4;

    private static String TAG = "ThreadUtils";
    private static final TimeUnit DEFAULT_UNIT = TimeUnit.SECONDS;

    private static ThreadPoolExecutorInfo innerExecutorInfo;
    private static ThreadPoolExecutorInfo localExecutorInfo;
    private static ThreadPoolExecutorInfo rightNowExecutorInfo;
    private static ThreadPoolExecutorInfo statisticsExecutorInfo;

    public static ScheduledThreadPoolExecutor getExecutorInfo(int type) {
        return _getExecutorInfo(type).get();
    }

    private static Runnable withDebug(final Runnable command) {
        return true ? () -> {
            Thread thread = Thread.currentThread();
            String currentProcessName = "";
            String threadName = thread.getName();
            long id = thread.getId();
            String runnableName = command.getClass().getName();
            Log.d(TAG, String.format("--> Thread start: [%s][%s][%s][%s]", threadName, Long.valueOf(id), currentProcessName, runnableName));
            command.run();
            Log.d(TAG, String.format("--> Thread end: [%s][%s][%s][%s]", threadName, Long.valueOf(id), currentProcessName, runnableName));

        } : command;
    }

    private static synchronized ThreadPoolExecutorInfo _getExecutorInfo(int type) {
        switch (type) {
            case TYPE_INNER:
                if (innerExecutorInfo == null) {
                    innerExecutorInfo = new ThreadPoolExecutorInfo(type, INNER_THREAD_SIZE, INNER_THREAD_SIZE, DEFAULT_KEEP_ALIVE_TIME, DEFAULT_UNIT);
                }
                return innerExecutorInfo;
            case TYPE_RIGHT_NOW:
                if (rightNowExecutorInfo == null) {
                    long keepAlive = 120;
                    rightNowExecutorInfo = new ThreadPoolExecutorInfo(type, RIGHT_NOW_THREAD_SIZE, RIGHT_NOW_THREAD_SIZE, keepAlive, DEFAULT_UNIT);
                }
                return rightNowExecutorInfo;

            case TYPE_RIGHT_NOW_LOCAL:
                if (localExecutorInfo == null) {
                    localExecutorInfo = new ThreadPoolExecutorInfo(type, RIGHT_NOW_LOCAL_THREAD_SIZE, RIGHT_NOW_LOCAL_THREAD_SIZE, DEFAULT_KEEP_ALIVE_TIME, DEFAULT_UNIT);
                }
                return localExecutorInfo;

            case TYPE_STATISTICS:
                if (statisticsExecutorInfo == null) {
                    statisticsExecutorInfo = new ThreadPoolExecutorInfo(type, STATISTICS_THREAD_SIZE, STATISTICS_THREAD_SIZE, DEFAULT_KEEP_ALIVE_TIME, DEFAULT_UNIT);
                }
                return statisticsExecutorInfo;
            default:
                throw new IllegalArgumentException("Unsupported thread pool type: " + type);
        }
    }

    public static void shutDown(int type) {
        _getExecutorInfo(type).shutDown();
    }

    public static synchronized void shutDownAll() {
        synchronized (ThreadUtils.class) {
            if (innerExecutorInfo != null) {
                innerExecutorInfo.shutDown();
            }
            if (localExecutorInfo != null) {
                localExecutorInfo.shutDown();
            }
            if (rightNowExecutorInfo != null) {
                rightNowExecutorInfo.shutDown();
            }
            if(statisticsExecutorInfo != null) {
                statisticsExecutorInfo.shutDown();
            }
        }
    }

    public static void execute(int type, Runnable command) {
        if (command == null) {
            throw new IllegalArgumentException("command is null");
        }
        schedule(type, command, 0L, TimeUnit.NANOSECONDS);
    }


    public static ScheduledFuture<?> schedule(int type, Runnable command, long delay, TimeUnit unit) {
        if (command == null) {
            throw new IllegalArgumentException("command is null");
        }
        return getExecutorInfo(type).schedule(withDebug(command), delay, unit);
    }

    private static class ThreadPoolExecutorInfo {
        private final int type;
        private final int corePoolSize;
        private final int maximumPoolSize;
        private final long keepAliveTime;
        private final TimeUnit unit;
        private ThreadPoolExecutor executor;

        public ThreadPoolExecutorInfo(int type, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
            this.type  = type;
            this.corePoolSize  = corePoolSize;
            this.maximumPoolSize  = maximumPoolSize;
            this.keepAliveTime  = keepAliveTime;
            this.unit  = unit;
        }

        synchronized ThreadPoolExecutor get(){
            if (executor == null) {
                executor = new ThreadPoolExecutor(
                        "PEBLLA" + type,
                        corePoolSize,
                        maximumPoolSize,
                        keepAliveTime,
                        unit,
                        new LinkedBlockingQueue<>(),
                        new PebllaThreadFactory(type),
                        new RejectedHandler()
                );
                executor.allowCoreThreadTimeOut(true);
            }
            return executor;
        }

        synchronized void shutDown() {
            if (executor != null) {
                try {
                    executor.shutdownNow();
                } catch (Exception e) {
                    //TODO 日志打印
                } finally {
                    executor = null;
                }
            }
        }
    }


    private static final class PebllaThreadFactory implements ThreadFactory {
        private final AtomicInteger mCount = new AtomicInteger(1);
        private final int type;

        PebllaThreadFactory(int type) {
            this.type = type;
        }

        @Override
        public Thread newThread(Runnable r) {
            String threadName = "PEBLLA" + this.type + " #" + this.mCount.getAndIncrement();
            PebllaThread thread = new PebllaThread(r, threadName);
            thread.setPriority((type  == TYPE_RIGHT_NOW || type == TYPE_RIGHT_NOW_LOCAL) ? 10 : 1);
            return thread;
        }
    }


    private static final class PebllaThread extends Thread{
        PebllaThread(Runnable target, String name) {
            super(target, name);
        }
    }

    /**
     * 重写RejectedExecutionHandler异常处理
     * 当线程池无法处理新任务时，只有通过SafeRunnable创建的线程才会抛出异常
     */
    private static final class RejectedHandler implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable r, java.util.concurrent.ThreadPoolExecutor executor) {
            if (SafeExecutor.checkRejected(r)) {
                throw new RejectedExecutionException("Task " + r + " rejected from " + executor);
            }
        }
    }


}
