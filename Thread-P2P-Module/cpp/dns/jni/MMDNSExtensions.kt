package com.mmdns

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Kotlin扩展函数集合
 */

// ==================== 协程扩展 ====================

/**
 * 使用suspendCancellableCoroutine实现真正的挂起函数
 */
suspend fun MMDNSManager.resolveHostSuspendCancellable(hostname: String): Result<String> =
    suspendCancellableCoroutine { continuation ->
        resolveHostAsync(hostname) { ip, success ->
            if (success) {
                continuation.resume(Result.success(ip))
            } else {
                continuation.resume(Result.failure(DNSResolutionException("Failed to resolve: $hostname")))
            }
        }
    }

/**
 * Flow方式解析多个主机名
 */
fun MMDNSManager.resolveHostsFlow(hostnames: List<String>): Flow<Pair<String, String>> = flow {
    for (hostname in hostnames) {
        val ip = resolveHost(hostname)
        if (ip.isNotEmpty()) {
            emit(hostname to ip)
        }
    }
}

/**
 * 批量异步解析
 */
fun MMDNSManager.resolveHostsBatch(
    hostnames: List<String>,
    scope: CoroutineScope,
    onResult: (hostname: String, ip: String, success: Boolean) -> Unit
) {
    hostnames.forEach { hostname ->
        scope.launch(Dispatchers.IO) {
            resolveHostAsync(hostname) { ip, success ->
                onResult(hostname, ip, success)
            }
        }
    }
}

// ==================== 便捷扩展 ====================

/**
 * 快速初始化并配置
 */
fun MMDNSManager.quickInit(
    context: android.content.Context,
    config: DNSConfig.() -> Unit = {}
) {
    init(context)
    configure(config)
}

/**
 * 安全解析（返回Result）
 */
fun MMDNSManager.resolveHostSafe(hostname: String): Result<String> {
    return try {
        val ip = resolveHost(hostname)
        if (ip.isNotEmpty()) {
            Result.success(ip)
        } else {
            Result.failure(DNSResolutionException("Empty result for: $hostname"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}

/**
 * 解析并验证IP
 */
fun MMDNSManager.resolveAndValidate(hostname: String): String? {
    val ip = resolveHost(hostname)
    return if (ip.isNotEmpty() && isValidIP(ip)) ip else null
}

/**
 * 检查是否为有效IP地址
 */
fun isValidIP(ip: String): Boolean {
    // IPv4检查
    if (ip.contains('.')) {
        val parts = ip.split('.')
        if (parts.size == 4) {
            return parts.all { 
                it.toIntOrNull()?.let { num -> num in 0..255 } ?: false 
            }
        }
    }
    // IPv6检查（简化）
    if (ip.contains(':')) {
        return ip.count { it == ':' } >= 2
    }
    return false
}

// ==================== 数据类扩展 ====================

/**
 * DNS解析结果数据类
 */
data class DNSResult(
    val hostname: String,
    val ip: String,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 带元数据的解析
 */
fun MMDNSManager.resolveWithMetadata(hostname: String): DNSResult {
    val startTime = System.currentTimeMillis()
    val ip = resolveHost(hostname)
    val success = ip.isNotEmpty()
    
    return DNSResult(
        hostname = hostname,
        ip = ip,
        success = success,
        timestamp = System.currentTimeMillis() - startTime
    )
}

// ==================== 监控扩展 ====================

/**
 * 性能监控回调
 */
interface PerformanceMonitor {
    fun onResolutionComplete(hostname: String, duration: Long, success: Boolean)
}

/**
 * 带监控的解析
 */
fun MMDNSManager.resolveWithMonitoring(
    hostname: String,
    monitor: PerformanceMonitor
): String {
    val startTime = System.currentTimeMillis()
    val ip = resolveHost(hostname)
    val duration = System.currentTimeMillis() - startTime
    val success = ip.isNotEmpty()
    
    monitor.onResolutionComplete(hostname, duration, success)
    return ip
}

// ==================== 缓存扩展 ====================

/**
 * 预加载常用域名
 */
fun MMDNSManager.preloadDomains(
    domains: List<String>,
    scope: CoroutineScope
) {
    scope.launch(Dispatchers.IO) {
        domains.forEach { domain ->
            resolveHost(domain) // 触发缓存
        }
    }
}

/**
 * 刷新缓存
 */
fun MMDNSManager.refreshCache(domains: List<String>) {
    clearCache()
    domains.forEach { domain ->
        resolveHost(domain)
    }
}

// ==================== 异常类 ====================

/**
 * DNS解析异常
 */
class DNSResolutionException(message: String) : Exception(message)

/**
 * DNS初始化异常
 */
class DNSInitializationException(message: String) : Exception(message)

// ==================== 辅助函数 ====================

/**
 * 从URL提取域名
 */
fun String.extractDomain(): String {
    var domain = this
    
    // 移除协议
    if (domain.contains("://")) {
        domain = domain.substringAfter("://")
    }
    
    // 移除路径
    if (domain.contains("/")) {
        domain = domain.substringBefore("/")
    }
    
    // 移除端口
    if (domain.contains(":")) {
        domain = domain.substringBefore(":")
    }
    
    return domain
}

/**
 * URL便捷解析
 */
fun MMDNSManager.resolveURL(url: String): String {
    val domain = url.extractDomain()
    return resolveHost(domain)
}