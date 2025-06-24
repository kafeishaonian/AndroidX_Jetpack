package com.alaeatposapp.t2.task

/**
 * 固定间隔退避策略
 */
class FixedIntervalBackoff(
    private val delayTime: Long = 5_000
) : BackoffStrategy {
    override fun recordSuccess() = Unit
    override fun recordFailure() = delayTime
    override fun reset() = Unit
}