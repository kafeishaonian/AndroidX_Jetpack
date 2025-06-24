package com.alaeatposapp.t2.task

/**
 * 退避策略接口
 */
interface BackoffStrategy {
    /**
     * 记录成功
     */
    fun recordSuccess()

    /**
     * 记录失败，并返回下次执行延迟时间
     */
    fun recordFailure(): Long

    /**
     * 重置策略状态
     */
    fun reset()
}