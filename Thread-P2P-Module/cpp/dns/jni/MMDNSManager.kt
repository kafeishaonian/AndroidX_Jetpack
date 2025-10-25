package com.mmdns

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MMDNS DNS解析管理器
 * 提供智能DNS解析、缓存管理和性能监控
 */
class MMDNSManager private constructor() {
    
    companion object {
        @Volatile
        private var instance: MMDNSManager? = null
        
        /**
         * 获取单例实例
         */
        fun getInstance(): MMDNSManager {
            return instance ?: synchronized(this) {
                instance ?: MMDNSManager().also { instance = it }
            }
        }
        
        init {
            try {
                System.loadLibrary("mmdns")
            } catch (e: UnsatisfiedLinkError) {
                e.printStackTrace()
            }
        }
    }
    
    // ==================== Native方法声明 ====================
    
    /**
     * 初始化DNS服务
     */
    private external fun nativeInit()
    
    /**
     * 同步解析主机名
     * @param hostname 主机名
     * @return IP地址，失败返回空字符串
     */
    private external fun nativeResolveHost(hostname: String): String
    
    /**
     * 异步解析主机名
     * @param hostname 主机名
     * @param callback 回调接口
     */
    private external fun nativeResolveHostAsync(hostname: String, callback: DNSCallback)
    
    /**
     * 获取所有IP地址
     * @param hostname 主机名
     * @return IP地址数组
     */
    private external fun nativeGetAllIPs(hostname: String): Array<String>
    
    /**
     * 设置DoH服务器
     * @param server DoH服务器URL
     */
    private external fun nativeSetDohServer(server: String)
    
    /**
     * 设置网络状态
     * @param state 网络状态码
     */
    private external fun nativeSetNetworkState(state: Int)
    
    /**
     * 启用/禁用系统DNS
     */
    private external fun nativeEnableSystemDNS(enable: Boolean)
    
    /**
     * 启用/禁用HTTP DNS
     */
    private external fun nativeEnableHttpDNS(enable: Boolean)
    
    /**
     * 设置缓存目录
     */
    private external fun nativeSetCacheDir(dir: String)
    
    /**
     * 清空缓存
     */
    private external fun nativeClearCache()
    
    /**
     * 设置日志级别
     */
    private external fun nativeSetLogLevel(level: Int)
    
    // ==================== Kotlin友好接口 ====================
    
    private var initialized = false
    
    /**
     * 初始化DNS服务
     * @param context Android Context
     */
    fun init(context: Context) {
        if (initialized) return
        
        // 设置缓存目录
        val cacheDir = context.cacheDir.absolutePath + "/dns"
        nativeSetCacheDir(cacheDir)
        
        // 初始化
        nativeInit()
        initialized = true
    }
    
    /**
     * 同步解析主机名（阻塞调用）
     * @param hostname 主机名
     * @return IP地址，失败返回空字符串
     */
    fun resolveHost(hostname: String): String {
        checkInitialized()
        return nativeResolveHost(hostname)
    }
    
    /**
     * 协程方式解析主机名
     * @param hostname 主机名
     * @return IP地址，失败返回空字符串
     */
    suspend fun resolveHostSuspend(hostname: String): String = withContext(Dispatchers.IO) {
        resolveHost(hostname)
    }
    
    /**
     * 异步解析主机名
     * @param hostname 主机名
     * @param onResult 结果回调
     */
    fun resolveHostAsync(hostname: String, onResult: (ip: String, success: Boolean) -> Unit) {
        checkInitialized()
        nativeResolveHostAsync(hostname, object : DNSCallback {
            override fun onResult(ip: String, success: Boolean) {
                onResult(ip, success)
            }
        })
    }
    
    /**
     * 获取所有IP地址
     * @param hostname 主机名
     * @return IP地址列表
     */
    fun getAllIPs(hostname: String): List<String> {
        checkInitialized()
        return nativeGetAllIPs(hostname).toList()
    }
    
    /**
     * 设置DoH服务器
     * @param server DoH服务器URL（默认Google DNS）
     */
    fun setDohServer(server: String = "https://dns.google/dns-query") {
        nativeSetDohServer(server)
    }
    
    /**
     * 设置网络状态
     * @param state 网络状态
     */
    fun setNetworkState(state: NetworkState) {
        nativeSetNetworkState(state.value)
    }
    
    /**
     * 启用/禁用系统DNS
     */
    fun enableSystemDNS(enable: Boolean) {
        nativeEnableSystemDNS(enable)
    }
    
    /**
     * 启用/禁用HTTP DNS
     */
    fun enableHttpDNS(enable: Boolean) {
        nativeEnableHttpDNS(enable)
    }
    
    /**
     * 清空所有缓存
     */
    fun clearCache() {
        nativeClearCache()
    }
    
    /**
     * 设置日志级别
     */
    fun setLogLevel(level: LogLevel) {
        nativeSetLogLevel(level.value)
    }
    
    private fun checkInitialized() {
        if (!initialized) {
            throw IllegalStateException("MMDNSManager not initialized. Call init(context) first.")
        }
    }
}

/**
 * DNS回调接口
 */
interface DNSCallback {
    /**
     * 解析结果回调
     * @param ip IP地址
     * @param success 是否成功
     */
    fun onResult(ip: String, success: Boolean)
}

/**
 * 网络状态枚举
 */
enum class NetworkState(val value: Int) {
    UNKNOWN(0),
    WIFI(1),
    MOBILE_2G(2),
    MOBILE_3G(3),
    MOBILE_4G(4),
    MOBILE_5G(5)
}

/**
 * 日志级别枚举
 */
enum class LogLevel(val value: Int) {
    DEBUG(0),
    INFO(1),
    WARN(2),
    ERROR(3)
}