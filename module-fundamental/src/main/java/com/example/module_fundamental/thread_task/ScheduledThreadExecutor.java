package com.example.module_fundamental.thread_task;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

// ScheduledThreadExecutor 类实现了 ThreadExecutor 和 ScheduledExecutorService 接口
public final class ScheduledThreadExecutor implements ThreadExecutor, ScheduledExecutorService {
    // 委托对象，用于代理执行任务
    private final ExecutorDelegate delegate;

    // 构造函数，接收 executorType 参数
    public ScheduledThreadExecutor(int executorType) {
        this.delegate  = new ExecutorDelegate(executorType);
    }

    // 获取委托对象
    private ScheduledExecutorService getDelegate() {
        return delegate.getValue();
    }

    // 执行任务的方法，委托给 delegate 执行
    @Override
    public void execute(Runnable command) {
        getDelegate().execute(command);
    }

    // 禁止在 ThreadUtils 外部调用 shutdown 方法
    @Override
    public void shutdown() {
        try {
            throw new IllegalAccessException("shutdown is not allowed outside ThreadUtils");
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // 禁止在 ThreadUtils 外部调用 shutdownNow 方法
    @Override
    public List<Runnable> shutdownNow() {
        try {
            throw new IllegalAccessException("shutdownNow is not allowed outside ThreadUtils");
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    // 判断是否已关闭，委托给 delegate 判断
    @Override
    public boolean isShutdown() {
        return getDelegate().isShutdown();
    }

    // 判断是否已终止，委托给 delegate 判断
    @Override
    public boolean isTerminated() {
        return getDelegate().isTerminated();
    }

    // 等待任务终止，委托给 delegate 处理
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return getDelegate().awaitTermination(timeout, unit);
    }

    // 提交 Callable 任务，委托给 delegate 处理，并进行空值检查
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        Future<T> submit = getDelegate().submit(task);
        if (submit == null) {
            throw new IllegalStateException("delegate.submit(task)  should not be null");
        }
        return submit;
    }

    // 提交 Runnable 任务并指定结果，委托给 delegate 处理，并进行空值检查
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        Future<T> submit = getDelegate().submit(task, result);
        if (submit == null) {
            throw new IllegalStateException("delegate.submit(task,  result) should not be null");
        }
        return submit;
    }

    // 提交 Runnable 任务，委托给 delegate 处理，并进行空值检查
    @Override
    public Future<?> submit(Runnable task) {
        Future<?> submit = getDelegate().submit(task);
        if (submit == null) {
            throw new IllegalStateException("delegate.submit(task)  should not be null");
        }
        return submit;
    }

    // 执行所有 Callable 任务，委托给 delegate 处理，并进行空值检查
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        List<Future<T>> invokeAll = getDelegate().invokeAll(tasks);
        if (invokeAll == null) {
            throw new IllegalStateException("delegate.invokeAll(tasks)  should not be null");
        }
        return invokeAll;
    }

    // 在指定时间内执行所有 Callable 任务，委托给 delegate 处理，并进行空值检查
    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        List<Future<T>> invokeAll = getDelegate().invokeAll(tasks, timeout, unit);
        if (invokeAll == null) {
            throw new IllegalStateException("delegate.invokeAll(tasks,  timeout, unit) should not be null");
        }
        return invokeAll;
    }

    // 执行任意一个 Callable 任务，委托给 delegate 处理
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return getDelegate().invokeAny(tasks);
    }

    // 在指定时间内执行任意一个 Callable 任务，委托给 delegate 处理
    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return getDelegate().invokeAny(tasks, timeout, unit);
    }

    // 安排一个 Runnable 任务在指定延迟后执行，委托给 delegate 处理，并进行空值检查
    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        ScheduledFuture<?> schedule = getDelegate().schedule(command, delay, unit);
        if (schedule == null) {
            throw new IllegalStateException("delegate.schedule(command,  delay, unit) should not be null");
        }
        return schedule;
    }

    // 安排一个 Callable 任务在指定延迟后执行，委托给 delegate 处理，并进行空值检查
    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        ScheduledFuture<V> schedule = getDelegate().schedule(callable, delay, unit);
        if (schedule == null) {
            throw new IllegalStateException("delegate.schedule(callable,  delay, unit) should not be null");
        }
        return schedule;
    }

    // 安排一个 Runnable 任务以固定速率执行，委托给 delegate 处理，并进行空值检查
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        ScheduledFuture<?> scheduleAtFixedRate = getDelegate().scheduleAtFixedRate(command, initialDelay, period, unit);
        if (scheduleAtFixedRate == null) {
            throw new IllegalStateException("delegate.scheduleAtFixedRate(command,  initialDelay, period, unit) should not be null");
        }
        return scheduleAtFixedRate;
    }

    // 安排一个 Runnable 任务以固定延迟执行，委托给 delegate 处理，并进行空值检查
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        ScheduledFuture<?> scheduleWithFixedDelay = getDelegate().scheduleWithFixedDelay(command, initialDelay, delay, unit);
        if (scheduleWithFixedDelay == null) {
            throw new IllegalStateException("delegate.scheduleWithFixedDelay(command,  initialDelay, delay, unit) should not be null");
        }
        return scheduleWithFixedDelay;
    }
}