package com.example.router.bitmap

import android.graphics.Bitmap
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object BitmapMonitor {
    private const val TAG = "BitmapMonitor"

    private val monitorMap = Collections.synchronizedMap(WeakHashMap<Bitmap, Meta>())
    private val monitorExecutor = ScheduledThreadPoolExecutor(1)

    private var leakThresholdMs: Long = 30_000
    private var checkIntervalMs: Long = 10_000
    private var maxStackDepth: Int = 15
    private val isRunning = AtomicBoolean(false)
    private val gson = Gson()


    //配置监控器
    fun configure(thresholdSec: Int, intervalSec: Int, maxDepth: Int) {
        leakThresholdMs = TimeUnit.SECONDS.toMillis(thresholdSec.toLong())
        checkIntervalMs = TimeUnit.SECONDS.toMillis(intervalSec.toLong())
        maxStackDepth = maxDepth

        if (isRunning.compareAndSet(false, true)) {
            startPeriodicCheck()
        }
    }

    //根据Bitmap分配
    fun track(bitmap: Bitmap, stackTrace: Array<StackTraceElement>) {
        if (bitmap.isRecycled || monitorMap.containsKey(bitmap)) {
            return
        }

        val stack = processStackTrace(stackTrace)
        val meta = Meta(
            width = bitmap.width,
            height = bitmap.height,
            config = bitmap.config,
            byteCount = bitmap.byteCount,
            allocationTime = System.currentTimeMillis(),
            stackTrace = stack
        )
        monitorMap[bitmap] = meta
    }

    private fun processStackTrace(stackTrace: Array<StackTraceElement>): String {
        val sb = StringBuilder()
        var depth = 0

        for (element in stackTrace) {
            if (element.className.startsWith("com.example.router.bitmap.BitmapMonitor")) {
                continue
            }
            if (element.className.startsWith("java.lang.Thread")) {
                continue
            }
            if (depth++ >= maxStackDepth) {
                break
            }

            sb.append(element.toString()).append("\n")
        }
        return sb.toString().trim()
    }


    /**
     * 启动定时检查
     */
    private fun startPeriodicCheck() {
        monitorExecutor.scheduleWithFixedDelay(
            ::checkForLeaks,
            checkIntervalMs, checkIntervalMs,
            TimeUnit.MILLISECONDS
        )
    }

    private fun checkForLeaks() {
        val now = System.currentTimeMillis()
        val leakCandidates = mutableMapOf<Bitmap, Meta>()

        synchronized(monitorMap) {
            val iterator = monitorMap.entries.iterator()
            while (iterator.hasNext()) {
                val (bitmap, meta) = iterator.next()
                if (bitmap.isRecycled) {
                    iterator.remove()
                    continue
                }

                if (now - meta.allocationTime > leakThresholdMs) {
                    leakCandidates[bitmap] = meta
                    iterator.remove()
                }
            }
        }

        if (leakCandidates.isNotEmpty()) {
            reportLeaks(leakCandidates)
        }
    }

    private fun reportLeaks(leaks: Map<Bitmap, Meta>) {
        val report = mutableMapOf<String, Any>()
        val leakedBytes = leaks.values.sumOf { it.byteCount }
//        report["app_version"] = BuildConfig.VERSION_NAME
        report["device_model"] = android.os.Build.MODEL
        report["os_version"] = android.os.Build.VERSION.RELEASE
        report["total_leaked_bytes"] = leakedBytes
        report["leak_count"] = leaks.size

        val leakDetails = JsonArray()
        for ((bitmap, meta) in leaks) {
            val detail = JsonObject()
            detail.addProperty("bitmap_id", bitmap.hashCode().toString(16))
            detail.addProperty("width", meta.width)
            detail.addProperty("height", meta.height)
            detail.addProperty("byte_count", meta.byteCount)
            detail.addProperty("config", meta.config.toString())
            detail.addProperty("allocation_time", meta.allocationTime)
            detail.addProperty("survival_time", System.currentTimeMillis() - meta.allocationTime)
            detail.addProperty("stack_trace", meta.stackTrace)
            leakDetails.add(detail)
        }
        report["details"] = leakDetails

        //上传
        Log.e("LogLogLog", "report: ${gson.toJson(report)}")
    }


}