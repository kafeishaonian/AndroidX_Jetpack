package com.mmdns

/**
 * DNS配置类
 * 使用DSL模式配置DNS服务
 */
class DNSConfig {
    /**
     * DoH服务器URL
     */
    var dohServer: String = "https://dns.google/dns-query"
    
    /**
     * 是否启用系统DNS
     */
    var enableSystemDNS: Boolean = true
    
    /**
     * 是否启用HTTP DNS
     */
    var enableHttpDNS: Boolean = true
    
    /**
     * 缓存过期时间（秒）
     */
    var cacheExpireTime: Int = 3600
    
    /**
     * 线程数
     */
    var threadCount: Int = 4
    
    /**
     * 日志级别
     */
    var logLevel: LogLevel = LogLevel.INFO
    
    /**
     * 网络状态
     */
    var networkState: NetworkState = NetworkState.UNKNOWN
}

/**
 * DSL配置扩展函数
 */
fun MMDNSManager.configure(block: DNSConfig.() -> Unit) {
    val config = DNSConfig().apply(block)
    
    // 应用配置
    setDohServer(config.dohServer)
    enableSystemDNS(config.enableSystemDNS)
    enableHttpDNS(config.enableHttpDNS)
    setLogLevel(config.logLevel)
    setNetworkState(config.networkState)
}

/**
 * 快速配置预设
 */
object DNSPresets {
    /**
     * 默认配置
     */
    val DEFAULT = DNSConfig().apply {
        dohServer = "https://dns.google/dns-query"
        enableSystemDNS = true
        enableHttpDNS = true
        logLevel = LogLevel.INFO
    }
    
    /**
     * 高性能配置
     */
    val HIGH_PERFORMANCE = DNSConfig().apply {
        dohServer = "https://cloudflare-dns.com/dns-query"
        enableSystemDNS = true
        enableHttpDNS = true
        threadCount = 8
        cacheExpireTime = 7200
        logLevel = LogLevel.WARN
    }
    
    /**
     * 调试配置
     */
    val DEBUG = DNSConfig().apply {
        dohServer = "https://dns.google/dns-query"
        enableSystemDNS = true
        enableHttpDNS = true
        threadCount = 2
        logLevel = LogLevel.DEBUG
    }
    
    /**
     * 仅系统DNS（无DoH）
     */
    val SYSTEM_ONLY = DNSConfig().apply {
        enableSystemDNS = true
        enableHttpDNS = false
        logLevel = LogLevel.INFO
    }
}

/**
 * 应用预设配置
 */
fun MMDNSManager.applyPreset(preset: DNSConfig) {
    setDohServer(preset.dohServer)
    enableSystemDNS(preset.enableSystemDNS)
    enableHttpDNS(preset.enableHttpDNS)
    setLogLevel(preset.logLevel)
    setNetworkState(preset.networkState)
}