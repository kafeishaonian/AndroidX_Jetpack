package com.aj.mvvm.task.cycle_task

/**
 * 无退避策略
 */
class NoBackoffStrategy : BackoffStrategy {
    override fun recordSuccess() = Unit
    override fun recordFailure() = 0L
    override fun reset() = Unit
}
