# MMDNS API 参考文档

## 目录
- [C++ API](#c-api)
- [Kotlin API](#kotlin-api)
- [数据类型](#数据类型)
- [配置选项](#配置选项)

---

## C++ API

### MMDNSEntrance - 入口类

#### 获取实例
```cpp
auto dns = MMDNSEntranceImpl::getInstance("default");
```

#### 初始化
```cpp
void init();
```

#### DNS解析
```cpp
// 同步解析
std::string resolveHost(const std::string& hostname);

// 异步解析
void resolveHostAsync(const std::string& hostname, TaskCallback callback);

// 获取所有IP
std::vector<std::string> getAllIPs(const std::string& hostname);
```

#### 配置方法
```cpp
// DoH服务器
void setDohServer(const std::string& server);

// 启用/禁用DNS源
void enableSystemDNS(bool enable);
void enableHttpDNS(bool enable);
void enableLocalCache(bool enable);

// 缓存配置
void setCacheDir(const std::string& dir);
void setCacheSize(size_t size);
void setCacheExpireTime(int seconds);

// 线程配置
void setThreadCount(int count);

// 网络状态
void setNetworkState(MMDNSAppNetState state);

// 清理
void clear();

// 统计信息
MMDNSServer::ServerStats getStats() const;
```

---

### MMDNSHttpClient - HTTP客户端

#### 构造和配置
```cpp
MMDNSHttpClient client;

client.setTimeout(5);           // 总超时5秒
client.setConnectTimeout(3);    // 连接超时3秒
client.setVerifySSL(true);      // 验证SSL证书
client.setUserAgent("MMDNS/1.0");
```

#### 发送DoH请求
```cpp
std::string response = client.sendDohRequest(
    "https://dns.google/dns-query",
    "www.example.com",
    "A"  // 或 "AAAA" for IPv6
);
```

#### 解析响应
```cpp
std::vector<std::string> ips = client.parseDohResponse(response);
```

---

### MMDNSDualStackOptimizer - 双栈优化器

#### 创建和配置
```cpp
MMDNSDualStackOptimizer optimizer;

optimizer.setPreferIPv6(true);              // 优先IPv6
optimizer.setConnectionAttemptDelay(250);    // 连接延迟250ms
optimizer.setTestTimeout(2000);              // 测试超时2秒
```

#### 选择最佳IP
```cpp
std::string bestIP = optimizer.selectBestIP(ipv4List, ipv6List, 80);
```

#### 并行测试
```cpp
optimizer.testConnections(ips, 80, [](const auto& result) {
    std::cout << "IP: " << result.ip 
              << " RTT: " << result.rtt 
              << " Success: " << result.success << std::endl;
});
```

---

### ObjectPool - 对象池

#### 创建对象池
```cpp
ObjectPool<MMDNSHostModel> hostPool(10, 100);  // 初始10个，最大100个
```

#### 使用对象池
```cpp
// 获取对象
auto host = hostPool.acquire();

// 使用对象
if (host) {
    host->setHostname("www.example.com");
    // ...
}

// 释放对象
hostPool.release(host);
```

#### 统计信息
```cpp
size_t available = hostPool.availableCount();
size_t total = hostPool.totalCount();
```

---

### MMDNSConnectionPool - 连接池

#### 创建连接池
```cpp
MMDNSConnectionPool pool(6, 30000, 60000);
// 参数: 每主机最大连接数, 连接超时(ms), 空闲超时(ms)
```

#### 使用连接
```cpp
// 获取连接
CURL* curl = pool.acquire("dns.google");

// 使用连接
if (curl) {
    curl_easy_setopt(curl, CURLOPT_URL, "https://dns.google/dns-query");
    curl_easy_perform(curl);
}

// 释放连接
pool.release(curl);
```

#### 管理
```cpp
pool.cleanup();  // 清理空闲连接
pool.clear();    // 清空所有连接

auto stats = pool.getStats();
std::cout << "Total: " << stats.totalConnections << std::endl;
```

---

### MMDNSMonitor - 性能监控

#### 创建监控器
```cpp
MMDNSMonitor monitor;
monitor.setEnabled(true);
```

#### 记录事件
```cpp
// 记录DNS解析
monitor.recordResolution("www.google.com", 150.5, true, false);

// 记录测速
monitor.recordSpeedCheck("8.8.8.8", 50.2);

// 记录错误
monitor.recordError("TIMEOUT");
```

#### 更新统计
```cpp
monitor.updateCacheStats(150, 200);
monitor.updateResourceStats(1024000, 4, 10);
```

#### 获取指标
```cpp
auto metrics = monitor.getMetrics();

std::cout << "总请求: " << metrics.totalRequests << std::endl;
std::cout << "成功率: " << (metrics.successfulRequests * 100.0 / metrics.totalRequests) << "%" << std::endl;
std::cout << "平均时间: " << metrics.avgResolutionTime << "ms" << std::endl;
std::cout << "P95时间: " << metrics.p95ResolutionTime << "ms" << std::endl;
std::cout << "缓存命中率: " << metrics.cacheHitRate << "%" << std::endl;
```

#### 生成报告
```cpp
std::string report = monitor.generateReport();
std::cout << report << std::endl;
```

---

### MMDNSIPModel - IP模型（IPv6增强）

#### 创建IP模型
```cpp
auto ipModel = std::make_shared<MMDNSIPModel>("2001:db8::1", 80);
```

#### IPv6检测
```cpp
if (ipModel->isIPv6()) {
    std::cout << "这是IPv6地址" << std::endl;
    
    // 检查地址类型
    if (ipModel->isIPv6Global()) {
        std::cout << "全局单播地址" << std::endl;
    }
    else if (ipModel->isIPv6LinkLocal()) {
        std::cout << "链路本地地址" << std::endl;
    }
    else if (ipModel->isIPv6UniqueLocal()) {
        std::cout << "唯一本地地址" << std::endl;
    }
}
```

---

## Kotlin API

### MMDNSManager - 主要管理类

#### 获取实例
```kotlin
val dnsManager = MMDNSManager.getInstance()
```

#### 初始化
```kotlin
// 基本初始化
dnsManager.init(context)

// 快速初始化+配置
dnsManager.quickInit(context) {
    dohServer = "https://dns.google/dns-query"
    enableSystemDNS = true
    enableHttpDNS = true
    logLevel = LogLevel.DEBUG
}
```

#### DNS解析
```kotlin
// 同步解析（阻塞）
val ip = dnsManager.resolveHost("www.google.com")

// 异步解析（回调）
dnsManager.resolveHostAsync("www.github.com") { ip, success ->
    if (success) {
        println("IP: $ip")
    }
}

// 协程解析（推荐）
lifecycleScope.launch {
    val ip = dnsManager.resolveHostSuspend("www.example.com")
    println("IP: $ip")
}

// Result方式（安全）
val result = dnsManager.resolveHostSafe("www.example.com")
result.onSuccess { ip ->
    println("Success: $ip")
}.onFailure { error ->
    println("Error: ${error.message}")
}

// 获取所有IP
val allIPs = dnsManager.getAllIPs("www.google.com")
```

#### 配置方法
```kotlin
// DoH服务器
dnsManager.setDohServer("https://dns.google/dns-query")

// 启用/禁用
dnsManager.enableSystemDNS(true)
dnsManager.enableHttpDNS(true)

// 日志级别
dnsManager.setLogLevel(LogLevel.DEBUG)

// 网络状态
dnsManager.setNetworkState(NetworkState.WIFI)

// 清空缓存
dnsManager.clearCache()
```

---

### DSL配置

#### 使用配置DSL
```kotlin
dnsManager.configure {
    dohServer = "https://cloudflare-dns.com/dns-query"
    enableSystemDNS = true
    enableHttpDNS = true
    cacheExpireTime = 3600
    threadCount = 4
    logLevel = LogLevel.INFO
    networkState = NetworkState.WIFI
}
```

#### 使用预设配置
```kotlin
// 默认配置
dnsManager.applyPreset(DNSPresets.DEFAULT)

// 高性能配置
dnsManager.applyPreset(DNSPresets.HIGH_PERFORMANCE)

// 调试配置
dnsManager.applyPreset(DNSPresets.DEBUG)

// 仅系统DNS
dnsManager.applyPreset(DNSPresets.SYSTEM_ONLY)
```

---

### 扩展函数

#### 批量解析
```kotlin
val domains = listOf("www.google.com", "www.github.com", "www.example.com")

dnsManager.resolveHostsBatch(domains, lifecycleScope) { hostname, ip, success ->
    println("$hostname -> $ip (success: $success)")
}
```

#### Flow API
```kotlin
dnsManager.resolveHostsFlow(domains).collect { (hostname, ip) ->
    println("$hostname -> $ip")
}
```

#### 预加载
```kotlin
val commonDomains = listOf("api.example.com", "cdn.example.com")
dnsManager.preloadDomains(commonDomains, lifecycleScope)
```

#### URL解析
```kotlin
val url = "https://www.google.com/search?q=kotlin"
val ip = dnsManager.resolveURL(url)  // 自动提取域名
```

#### 带元数据的解析
```kotlin
val result = dnsManager.resolveWithMetadata("www.example.com")
println("""
    Hostname: ${result.hostname}
    IP: ${result.ip}
    Success: ${result.success}
    Duration: ${result.timestamp}ms
""".trimIndent())
```

#### 性能监控
```kotlin
val monitor = object : PerformanceMonitor {
    override fun onResolutionComplete(hostname: String, duration: Long, success: Boolean) {
        println("$hostname took ${duration}ms")
    }
}

val ip = dnsManager.resolveWithMonitoring("www.google.com", monitor)
```

---

## 数据类型

### NetworkState (网络状态)
```kotlin
enum class NetworkState(val value: Int) {
    UNKNOWN(0),
    WIFI(1),
    MOBILE_2G(2),
    MOBILE_3G(3),
    MOBILE_4G(4),
    MOBILE_5G(5)
}
```

### LogLevel (日志级别)
```kotlin
enum class LogLevel(val value: Int) {
    DEBUG(0),
    INFO(1),
    WARN(2),
    ERROR(3)
}
```

### DNSResult (解析结果)
```kotlin
data class DNSResult(
    val hostname: String,
    val ip: String,
    val success: Boolean,
    val timestamp: Long
)
```

### DNSCallback (回调接口)
```kotlin
interface DNSCallback {
    fun onResult(ip: String, success: Boolean)
}
```

### PerformanceMonitor (性能监控)
```kotlin
interface PerformanceMonitor {
    fun onResolutionComplete(hostname: String, duration: Long, success: Boolean)
}
```

---

## 配置选项

### DNS配置 (DNSConfig)

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| dohServer | String | "https://dns.google/dns-query" | DoH服务器URL |
| enableSystemDNS | Boolean | true | 是否启用系统DNS |
| enableHttpDNS | Boolean | true | 是否启用HTTP DNS |
| cacheExpireTime | Int | 3600 | 缓存过期时间（秒） |
| threadCount | Int | 4 | 工作线程数 |
| logLevel | LogLevel | INFO | 日志级别 |
| networkState | NetworkState | UNKNOWN | 当前网络状态 |

### 性能参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| HTTP超时 | 5秒 | HTTP请求总超时 |
| 连接超时 | 3秒 | HTTP连接超时 |
| 测速超时 | 2秒 | IP测速超时 |
| 对象池大小 | 10/100 | 初始/最大对象数 |
| 连接池大小 | 6/主机 | 每主机最大连接数 |
| 空闲超时 | 60秒 | 连接空闲超时 |

---

## 常用场景示例

### 场景1: 基本DNS解析
```kotlin
val dnsManager = MMDNSManager.getInstance()
dnsManager.init(context)

val ip = dnsManager.resolveHost("www.google.com")
println("IP: $ip")
```

### 场景2: 异步批量解析
```kotlin
val domains = listOf("www.google.com", "www.github.com", "www.stackoverflow.com")

dnsManager.resolveHostsBatch(domains, lifecycleScope) { hostname, ip, success ->
    if (success) {
        println("$hostname -> $ip")
    }
}
```

### 场景3: 协程Flow
```kotlin
lifecycleScope.launch {
    dnsManager.resolveHostsFlow(domains).collect { (hostname, ip) ->
        updateUI(hostname, ip)
    }
}
```

### 场景4: 网络切换适配
```kotlin
fun onNetworkChanged(networkType: NetworkType) {
    val state = when (networkType) {
        NetworkType.WIFI -> NetworkState.WIFI
        NetworkType.MOBILE -> NetworkState.MOBILE_4G
        else -> NetworkState.UNKNOWN
    }
    
    dnsManager.setNetworkState(state)
    
    // WiFi环境启用DoH，移动网络禁用节省流量
    if (state == NetworkState.WIFI) {
        dnsManager.enableHttpDNS(true)
    } else {
        dnsManager.enableHttpDNS(false)
    }
}
```

### 场景5: 预加载常用域名
```kotlin
// Application onCreate中
val commonDomains = listOf(
    "api.myapp.com",
    "cdn.myapp.com",
    "analytics.myapp.com"
)
dnsManager.preloadDomains(commonDomains, GlobalScope)
```

### 场景6: 性能监控
```kotlin
class DNSPerformanceTracker : PerformanceMonitor {
    private val stats = mutableMapOf<String, MutableList<Long>>()
    
    override fun onResolutionComplete(hostname: String, duration: Long, success: Boolean) {
        stats.getOrPut(hostname) { mutableListOf() }.add(duration)
        
        // 分析性能
        val times = stats[hostname]!!
        val avg = times.average()
        if (avg > 200) {
            Log.w("DNS", "$hostname resolution is slow: ${avg}ms")
        }
    }
}

val tracker = DNSPerformanceTracker()
val ip = dnsManager.resolveWithMonitoring("www.google.com", tracker)
```

---

## 最佳实践

### 1. 初始化时机
```kotlin
// 在Application的onCreate中初始化
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        MMDNSManager.getInstance().quickInit(this) {
            // 配置...
        }
    }
}
```

### 2. 协程使用
```kotlin
// 使用suspend函数，避免阻塞主线程
lifecycleScope.launch {
    val ip = dnsManager.resolveHostSuspend("www.example.com")
    // UI更新
}
```

### 3. 错误处理
```kotlin
val result = dnsManager.resolveHostSafe("www.example.com")
result.onSuccess { ip ->
    // 成功处理
}.onFailure { error ->
    // 错误处理
    Log.e("DNS", "Resolution failed", error)
}
```

### 4. 网络状态管理
```kotlin
// 监听网络变化
networkCallback.onAvailable { network ->
    dnsManager.setNetworkState(NetworkState.WIFI)
    dnsManager.clearCache()  // 网络切换时清空缓存
}
```

### 5. 性能优化建议
- 使用预加载减少首次解析延迟
- WiFi环境启用DoH提高安全性
- 移动网络禁用DoH节省流量
- 定期清理缓存避免过期数据
- 使用对象池和连接池提升性能

---

## 性能指标参考

### 解析时间
- **缓存命中**: < 1ms
- **系统DNS**: 50-100ms
- **HTTP DNS**: 100-200ms
- **含测速**: 200-500ms

### 资源使用
- **内存**: 5-10MB
- **线程**: 4-8个
- **CPU**: < 5%

### 可靠性
- **成功率**: > 99%
- **缓存命中率**: > 80%
- **并发QPS**: > 1000

---

## 故障排查

### 问题1: DoH请求失败
**原因**: libcurl未正确链接或SSL证书验证失败  
**解决**: 检查CMakeLists.txt中的库链接，禁用SSL验证（仅测试）

### 问题2: IPv6解析失败
**原因**: 网络不支持IPv6  
**解决**: 使用双栈优化器自动回退到IPv4

### 问题3: 内存占用过高
**原因**: 对象池或缓存过大  
**解决**: 调整对象池大小和缓存容量

### 问题4: 解析速度慢
**原因**: 未使用缓存或测速超时设置过长  
**解决**: 启用缓存，调整超时参数

---

## 版本兼容性

- **C++ 标准**: C++14或更高
- **Android API**: 21+ (Android 5.0+)
- **Kotlin**: 1.9+
- **Coroutines**: 1.6+
- **NDK**: r21+

---

**文档版本**: 2.0  
**最后更新**: 2025-01-25  
**适用版本**: MMDNS v2.0+