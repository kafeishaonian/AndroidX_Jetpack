package com.alaeatposapp.t2.task

/**
 * 指数退避策略实现
 */
class ExponentialBackoff(
    private val minDelay: Long = 5_000,
    private val maxDelay: Long = 30_000,
    private val maxRetries: Int = 10
) : BackoffStrategy {

    private var retryCount = 0

    override fun recordSuccess() = reset()

    override fun recordFailure(): Long {
        retryCount = (retryCount + 1).coerceAtMost(maxRetries)
        val delay = minDelay * (1 shl retryCount)
        return delay.coerceAtMost(maxDelay)
    }

    override fun reset() {
        retryCount = 0
    }

}