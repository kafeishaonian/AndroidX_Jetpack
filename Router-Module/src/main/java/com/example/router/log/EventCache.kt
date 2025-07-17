package com.example.router.log

import java.util.concurrent.ConcurrentLinkedQueue

/**
 * 本地缓存上报
 */
object EventCache {

    private val queue = ConcurrentLinkedQueue<EventData>()

    //批次上报阈值
    private const val BATCH_SIZE = 10
    // 定时上报间隔（秒）
    private const val INTERVAL_SECONDS = 5L

    @JvmStatic
    fun addEvent(event: EventData) {
        queue.offer(event)

        if (queue.size >= BATCH_SIZE) {
            flushBatch()
        }
    }

    @Synchronized
    private fun flushBatch() {
        if (queue.isEmpty()) return
        val batch = mutableListOf<EventData>()
        repeat(minOf(BATCH_SIZE, queue.size)) {
            queue.poll()?.let {
                batch.add(it)
            }
        }
        if (batch.isNotEmpty()) {
            //TODO 上报接口
        }
    }
}