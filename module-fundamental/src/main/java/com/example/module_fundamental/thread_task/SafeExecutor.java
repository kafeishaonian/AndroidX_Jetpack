package com.example.module_fundamental.thread_task;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SafeExecutor implements ScheduledExecutorService {

    private final ScheduledExecutorService mDelegate;
    private static volatile Field mCallableField;
    private static volatile Class<?> mRunnableAdapterClass;
    private static volatile Field mTaskField;

    public SafeExecutor(ScheduledExecutorService delegate) {
        mDelegate = delegate;
    }


    public static boolean checkRejected(Runnable runnable) {
        if (!(runnable instanceof FutureTask<?>)) {
            return false;
        }
        try {
            Field callableField = getCallableField();
            if (callableField == null) {
                return false;
            }
            Object callable = callableField.get(runnable);
            if (callable == null || !getRunnableAdapterClass().isInstance(callable)) {
                return false;
            }

            Field takeField = getTaskField();
            if (takeField == null) {
                return false;
            }
            Object realTask = takeField.get(callable);
            if (realTask == null) {
                return false;
            }
            return (realTask instanceof SafeRunnable);
        } catch (Exception e) {
            return false;
        }
    }

    private static Field getTaskField() {
        try {
            if (mTaskField == null) {
                synchronized (SafeExecutor.class) {
                    if (mTaskField == null) {
                        mTaskField = getRunnableAdapterClass().getDeclaredField("task");
                        mTaskField.setAccessible(true);
                    }
                }
            }
            return mTaskField;
        } catch (Exception e) {
            return null;
        }

    }


    private static Field getCallableField() {
        try {
            if (mCallableField == null) {
                synchronized (SafeExecutor.class) {
                    if (mCallableField == null) {
                        mCallableField = FutureTask.class.getDeclaredField("callable");
                        mCallableField.setAccessible(true);
                    }
                }
            }
            return mCallableField;
        } catch (Exception e) {
            return null;
        }
    }



    private static Class<?> getRunnableAdapterClass() {
        if (mRunnableAdapterClass == null) {
            synchronized (SafeExecutor.class) {
                if (mRunnableAdapterClass == null) {
                    mRunnableAdapterClass = Executors.callable(() -> {}).getClass();
                }
            }
        }
        return mRunnableAdapterClass;
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return mDelegate.schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return mDelegate.schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return mDelegate.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return mDelegate.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public void shutdown() {

    }

    @Override
    public List<Runnable> shutdownNow() {
        return Collections.emptyList();
    }

    @Override
    public boolean isShutdown() {
        return mDelegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return mDelegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return mDelegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return mDelegate.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return mDelegate.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return mDelegate.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return mDelegate.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return mDelegate.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException, InterruptedException {
        return mDelegate.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws ExecutionException, InterruptedException, TimeoutException {
        return mDelegate.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        if (command != null) {
            mDelegate.execute(new SafeRunnable(command));
        } else {
            mDelegate.execute(command);
        }
    }


    private static class SafeRunnable implements Runnable {
        private Runnable mRunnable;
        public SafeRunnable(Runnable delegate) {
            mRunnable = delegate;
        }

        @Override
        public void run() {
            mRunnable.run();
        }
    }
}
