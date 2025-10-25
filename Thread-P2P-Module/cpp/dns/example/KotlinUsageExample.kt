package com.mmdns.example

import android.app.Application
import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.mmdns.*
import kotlinx.coroutines.launch

/**
 * MMDNS Kotlin API 使用示例
 */

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 示例1: 基本初始化和配置
        basicSetup()
        
        // 示例2: 使用DSL配置
        dslConfiguration()
        
        // 示例3: 使用预设配置
        presetConfiguration()
    }
    
    /**
     * 示例1: 基本初始化和配置
     */
    private fun basicSetup() {
        val dnsManager = MMDNSManager.getInstance()
        
        // 初始化
        dnsManager.init(this)
        
        // 配置DoH服务器
        dnsManager.setDohServer("https://dns.google/dns-query")
        
        // 启用功能
        dnsManager.enableSystemDNS(true)
        dnsManager.enableHttpDNS(true)
        
        // 设置日志级别
        dnsManager.setLogLevel(LogLevel.DEBUG)
        
        // 设置网络状态
        dnsManager.setNetworkState(NetworkState.WIFI)
    }
    
    /**
     * 示例2: 使用DSL配置
     */
    private fun dslConfiguration() {
        MMDNSManager.getInstance().apply {
            init(this@MyApplication)
            
            configure {
                dohServer = "https://cloudflare-dns.com/dns-query"
                enableSystemDNS = true
                enableHttpDNS = true
                cacheExpireTime = 3600
                threadCount = 4
                logLevel = LogLevel.INFO
                networkState = NetworkState.WIFI
            }
        }
    }
    
    /**
     * 示例3: 使用预设配置
     */
    private fun presetConfiguration() {
        MMDNSManager.getInstance().apply {
            init(this@MyApplication)
            applyPreset(DNSPresets.HIGH_PERFORMANCE)
        }
    }
}

/**
 * Activity中的使用示例
 */
class DNSExampleActivity : androidx.appcompat.app.AppCompatActivity() {
    
    private val dnsManager = MMDNSManager.getInstance()
    
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 示例4: 同步解析
        syncResolve()
        
        // 示例5: 异步解析
        asyncResolve()
        
        // 示例6: 协程解析
        coroutineResolve()
        
        // 示例7: 批量解析
        batchResolve()
        
        // 示例8: Flow解析
        flowResolve()
        
        // 示例9: 带监控的解析
        resolveWithMonitoring()
        
        // 示例10: 预加载域名
        preloadDomains()
    }
    
    /**
     * 示例4: 同步解析（阻塞调用）
     */
    private fun syncResolve() {
        Thread {
            val ip = dnsManager.resolveHost("www.google.com")
            println("Resolved IP: $ip")
            
            // 获取所有IP
            val allIPs = dnsManager.getAllIPs("www.google.com")
            println("All IPs: $allIPs")
        }.start()
    }
    
    /**
     * 示例5: 异步解析（回调方式）
     */
    private fun asyncResolve() {
        dnsManager.resolveHostAsync("www.github.com") { ip, success ->
            if (success) {
                println("Async resolved: $ip")
            } else {
                println("Async resolution failed")
            }
        }
    }
    
    /**
     * 示例6: 协程解析
     */
    private fun coroutineResolve() {
        lifecycleScope.launch {
            // 方式1: suspend函数
            val ip = dnsManager.resolveHostSuspend("www.example.com")
            println("Coroutine resolved: $ip")
            
            // 方式2: Result包装
            val result = dnsManager.resolveHostSuspendCancellable("www.baidu.com")
            result.onSuccess { ip ->
                println("Success: $ip")
            }.onFailure { error ->
                println("Error: ${error.message}")
            }
        }
    }
    
    /**
     * 示例7: 批量解析
     */
    private fun batchResolve() {
        val domains = listOf(
            "www.google.com",
            "www.github.com",
            "www.stackoverflow.com"
        )
        
        dnsManager.resolveHostsBatch(domains, lifecycleScope) { hostname, ip, success ->
            println("$hostname -> $ip (success: $success)")
        }
    }
    
    /**
     * 示例8: Flow解析
     */
    private fun flowResolve() {
        val domains = listOf(
            "www.google.com",
            "www.github.com",
            "www.example.com"
        )
        
        lifecycleScope.launch {
            dnsManager.resolveHostsFlow(domains).collect { (hostname, ip) ->
                println("Flow: $hostname -> $ip")
            }
        }
    }
    
    /**
     * 示例9: 带监控的解析
     */
    private fun resolveWithMonitoring() {
        val monitor = object : PerformanceMonitor {
            override fun onResolutionComplete(hostname: String, duration: Long, success: Boolean) {
                println("Monitor: $hostname took ${duration}ms, success=$success")
            }
        }
        
        val ip = dnsManager.resolveWithMonitoring("www.google.com", monitor)
        println("Monitored result: $ip")
    }
    
    /**
     * 示例10: 预加载常用域名
     */
    private fun preloadDomains() {
        val commonDomains = listOf(
            "www.google.com",
            "www.github.com",
            "api.example.com"
        )
        
        dnsManager.preloadDomains(commonDomains, lifecycleScope)
    }
    
    /**
     * 示例11: URL解析
     */
    private fun resolveURL() {
        val url = "https://www.google.com/search?q=kotlin"
        val ip = dnsManager.resolveURL(url)
        println("URL resolved: $ip")
    }
    
    /**
     * 示例12: 安全解析（返回Result）
     */
    private fun safeResolve() {
        val result = dnsManager.resolveHostSafe("www.example.com")
        result.onSuccess { ip ->
            println("Safe resolve success: $ip")
        }.onFailure { error ->
            println("Safe resolve failed: ${error.message}")
        }
    }
    
    /**
     * 示例13: 解析并验证
     */
    private fun resolveAndValidate() {
        val ip = dnsManager.resolveAndValidate("www.google.com")
        if (ip != null) {
            println("Valid IP: $ip")
        } else {
            println("Invalid or failed resolution")
        }
    }
    
    /**
     * 示例14: 带元数据的解析
     */
    private fun resolveWithMetadata() {
        val result = dnsManager.resolveWithMetadata("www.github.com")
        println("""
            Hostname: ${result.hostname}
            IP: ${result.ip}
            Success: ${result.success}
            Duration: ${result.timestamp}ms
        """.trimIndent())
    }
    
    /**
     * 示例15: 快速初始化
     */
    private fun quickInit(context: Context) {
        dnsManager.quickInit(context) {
            dohServer = "https://dns.google/dns-query"
            enableSystemDNS = true
            enableHttpDNS = true
            logLevel = LogLevel.DEBUG
        }
    }
    
    /**
     * 示例16: 缓存管理
     */
    private fun cacheManagement() {
        // 清空缓存
        dnsManager.clearCache()
        
        // 刷新特定域名的缓存
        val domains = listOf("www.google.com", "www.github.com")
        dnsManager.refreshCache(domains)
    }
}

/**
 * 高级使用示例
 */
object AdvancedExamples {
    
    /**
     * 自定义性能监控
     */
    class CustomMonitor : PerformanceMonitor {
        private val stats = mutableMapOf<String, MutableList<Long>>()
        
        override fun onResolutionComplete(hostname: String, duration: Long, success: Boolean) {
            stats.getOrPut(hostname) { mutableListOf() }.add(duration)
        }
        
        fun getAverageTime(hostname: String): Double {
            val times = stats[hostname] ?: return 0.0
            return times.average()
        }
        
        fun printStats() {
            stats.forEach { (hostname, times) ->
                println("$hostname: avg=${times.average()}ms, count=${times.size}")
            }
        }
    }
    
    /**
     * 智能DNS切换
     */
    fun smartDNSSwitch(networkState: NetworkState) {
        val dnsManager = MMDNSManager.getInstance()
        
        when (networkState) {
            NetworkState.WIFI -> {
                // WiFi环境使用HTTP DNS
                dnsManager.configure {
                    enableSystemDNS = true
                    enableHttpDNS = true
                    dohServer = "https://dns.google/dns-query"
                }
            }
            NetworkState.MOBILE_4G, NetworkState.MOBILE_5G -> {
                // 移动网络使用系统DNS（节省流量）
                dnsManager.configure {
                    enableSystemDNS = true
                    enableHttpDNS = false
                }
            }
            else -> {
                // 其他情况只使用系统DNS
                dnsManager.configure {
                    enableSystemDNS = true
                    enableHttpDNS = false
                }
            }
        }
        
        dnsManager.setNetworkState(networkState)
    }
}