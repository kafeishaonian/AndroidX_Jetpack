# MMDNS 可扩展功能实施方案

## 📋 项目概览

基于现有的MMDNS DNS解析库，实施一系列可扩展功能，提升性能、功能完整性和易用性。

### 当前项目状态

- ✅ **核心C++实现**: 100% 完成
  - DNS解析核心（系统DNS、HTTP DNS框架、本地缓存）
  - 智能测速系统（Socket + Ping）
  - 线程池和任务调度
  - LRU缓存和数据持久化
  
- ⚠️ **JNI层**: 50% 完成
  - C++ JNI绑定已实现 ([`mmdns_jni.cpp`](Thread-P2P-Module/cpp/dns/jni/mmdns_jni.cpp))
  - Java/Kotlin接口层缺失
  
- ⚠️ **HTTP DNS**: 需要增强
  - DoH框架已就绪
  - 缺少HTTP客户端实现
  
- ✅ **预编译库**: 已准备
  - libcurl (arm64-v8a)
  - OpenSSL (arm64-v8a)
  - zlib (arm64-v8a)

---

## 🎯 可扩展功能清单

### 1. HTTP客户端集成 (libcurl)

**目标**: 完善HTTP DNS over HTTPS (DoH)功能

**实施内容**:
- 集成prebuilt libcurl静态库
- 实现HTTP DNS请求和响应解析
- 支持Google DNS、Cloudflare DNS等公共DoH服务
- 添加SSL/TLS支持（使用OpenSSL）
- 实现DNS over HTTPS协议（RFC 8484）

**技术细节**:
```cpp
// 新增文件: include/MMDNSHttpClient.h
class MMDNSHttpClient {
public:
    // 发送DoH请求
    std::string sendDohRequest(const std::string& url, 
                               const std::string& hostname,
                               const std::string& recordType);
    
    // 解析DoH响应 (JSON格式)
    std::vector<std::string> parseDohResponse(const std::string& json);
    
    // 连接池管理
    void initConnectionPool(int maxConnections);
    CURL* acquireConnection();
    void releaseConnection(CURL* handle);
    
private:
    std::vector<CURL*> connectionPool_;
    std::mutex poolMutex_;
};
```

**集成点**:
- 修改 [`MMDNSServerHandle.cpp`](Thread-P2P-Module/cpp/dns/src/MMDNSServerHandle.cpp:113-127)
- 更新 [`CMakeLists.txt`](Thread-P2P-Module/cpp/dns/CMakeLists.txt) 链接libcurl

---

### 2. JNI绑定层完善

**目标**: 创建完整的Kotlin接口层

**实施内容**:

#### 2.1 Kotlin核心接口
```kotlin
// 文件: jni/MMDNSManager.kt
package com.mmdns

import android.content.Context

/**
 * MMDNS DNS解析管理器
 */
class MMDNSManager private constructor() {
    
    companion object {
        @Volatile
        private var instance: MMDNSManager? = null
        
        fun getInstance(): MMDNSManager {
            return instance ?: synchronized(this) {
                instance ?: MMDNSManager().also { instance = it }
            }
        }
        
        init {
            System.loadLibrary("mmdns")
        }
    }
    
    // DNS解析
    external fun nativeInit()
    external fun nativeResolveHost(hostname: String): String
    external fun nativeResolveHostAsync(hostname: String, callback: DNSCallback)
    external fun nativeGetAllIPs(hostname: String): Array<String>
    
    // 配置
    external fun nativeSetDohServer(server: String)
    external fun nativeSetNetworkState(state: Int)
    external fun nativeEnableSystemDNS(enable: Boolean)
    external fun nativeEnableHttpDNS(enable: Boolean)
    external fun nativeSetCacheDir(dir: String)
    external fun nativeClearCache()
    external fun nativeSetLogLevel(level: Int)
    
    // Kotlin友好接口
    fun init(context: Context) {
        nativeSetCacheDir(context.cacheDir.absolutePath + "/dns")
        nativeInit()
    }
    
    suspend fun resolveHost(hostname: String): String = 
        withContext(Dispatchers.IO) {
            nativeResolveHost(hostname)
        }
    
    fun resolveHostAsync(hostname: String, onResult: (String, Boolean) -> Unit) {
        nativeResolveHostAsync(hostname, object : DNSCallback {
            override fun onResult(ip: String, success: Boolean) {
                onResult(ip, success)
            }
        })
    }
}

/**
 * DNS回调接口
 */
interface DNSCallback {
    fun onResult(ip: String, success: Boolean)
}

/**
 * DNS配置DSL
 */
class DNSConfig {
    var dohServer: String = "https://dns.google/dns-query"
    var enableSystemDNS: Boolean = true
    var enableHttpDNS: Boolean = true
    var cacheExpireTime: Int = 3600
    var threadCount: Int = 4
    var logLevel: LogLevel = LogLevel.INFO
}

enum class LogLevel(val value: Int) {
    DEBUG(0),
    INFO(1),
    WARN(2),
    ERROR(3)
}

/**
 * Kotlin DSL配置
 */
fun MMDNSManager.configure(block: DNSConfig.() -> Unit) {
    val config = DNSConfig().apply(block)
    nativeSetDohServer(config.dohServer)
    nativeEnableSystemDNS(config.enableSystemDNS)
    nativeEnableHttpDNS(config.enableHttpDNS)
    nativeSetLogLevel(config.logLevel.value)
}
```

#### 2.2 回调机制增强
- 使用GlobalRef管理Java对象生命周期
- 实现线程安全的回调队列
- 支持lambda和函数引用

---

### 3. IPv6支持增强

**目标**: 完善IPv6解析和双栈支持

**实施内容**:

#### 3.1 IPv6解析优化
```cpp
// 修改: include/MMDNSIPModel.h
class MMDNSIPModel {
public:
    enum class IPVersion {
        IPv4,
        IPv6,
        UNKNOWN
    };
    
    IPVersion getIPVersion() const;
    bool isIPv4() const;
    bool isIPv6() const;
    
    // IPv6特定功能
    std::string getIPv6Compressed() const;
    bool isIPv6LinkLocal() const;
    bool isIPv6SiteLocal() const;
    bool isIPv6UniqueLocal() const;
    
private:
    IPVersion ipVersion_;
    void detectIPVersion();
};
```

#### 3.2 双栈优化策略
```cpp
// 新增: include/MMDNSDualStackOptimizer.h
class MMDNSDualStackOptimizer {
public:
    // Happy Eyeballs算法 (RFC 8305)
    std::string selectBestIP(
        const std::vector<std::shared_ptr<MMDNSIPModel>>& ipv4List,
        const std::vector<std::shared_ptr<MMDNSIPModel>>& ipv6List,
        int port
    );
    
    // 并行连接测试
    void testConnections(
        const std::vector<std::shared_ptr<MMDNSIPModel>>& ips,
        int port,
        std::function<void(const std::string&)> callback
    );
    
private:
    // IPv6优先策略
    bool preferIPv6_;
    // 连接尝试延迟（毫秒）
    int connectionAttemptDelay_;
};
```

#### 3.3 DNS解析增强
- 同时请求A和AAAA记录
- 智能选择IPv4/IPv6
- 网络环境自适应

---

### 4. 性能优化

**目标**: 提升性能和资源利用效率

#### 4.1 对象池
```cpp
// 新增: include/MMDNSObjectPool.h
template<typename T>
class ObjectPool {
public:
    explicit ObjectPool(size_t initialSize = 10, size_t maxSize = 100);
    
    std::shared_ptr<T> acquire();
    void release(std::shared_ptr<T> obj);
    
    size_t size() const;
    size_t availableCount() const;
    
private:
    std::vector<std::shared_ptr<T>> pool_;
    std::mutex mutex_;
    size_t maxSize_;
    
    std::shared_ptr<T> createObject();
};

// 特化实例
using HostModelPool = ObjectPool<MMDNSHostModel>;
using IPModelPool = ObjectPool<MMDNSIPModel>;
using SocketPool = ObjectPool<MMDNSSocket>;
```

#### 4.2 连接池
```cpp
// 修改: include/MMDNSHttpClient.h
class ConnectionPool {
public:
    struct Connection {
        CURL* handle;
        bool inUse;
        std::chrono::steady_clock::time_point lastUsed;
    };
    
    CURL* acquire(const std::string& host);
    void release(CURL* handle);
    void cleanup();  // 清理空闲连接
    
private:
    std::map<std::string, std::vector<Connection>> pools_;
    std::mutex mutex_;
    int maxConnectionsPerHost_ = 6;
    int connectionTimeout_ = 30000;  // 30秒
};
```

#### 4.3 内存优化
- 实现对象复用机制
- 减少内存分配次数
- 智能缓存大小调整

---

### 5. 性能监控系统

**目标**: 提供完善的性能指标和监控

```cpp
// 新增: include/MMDNSMonitor.h
class MMDNSMonitor {
public:
    struct Metrics {
        // DNS解析指标
        uint64_t totalRequests;
        uint64_t successfulRequests;
        uint64_t failedRequests;
        uint64_t cachedRequests;
        
        // 性能指标
        double avgResolutionTime;      // 平均解析时间（毫秒）
        double avgSpeedCheckTime;      // 平均测速时间（毫秒）
        double p50ResolutionTime;      // P50解析时间
        double p95ResolutionTime;      // P95解析时间
        double p99ResolutionTime;      // P99解析时间
        
        // 缓存指标
        double cacheHitRate;           // 缓存命中率
        size_t cacheSize;              // 当前缓存大小
        size_t cacheCapacity;          // 缓存容量
        
        // 资源使用
        size_t memoryUsage;            // 内存使用（字节）
        int activeThreads;             // 活跃线程数
        int queuedTasks;               // 队列中的任务数
        
        // 错误统计
        std::map<std::string, uint64_t> errorCounts;
        
        // 时间窗口统计
        std::vector<double> recentResolutionTimes;  // 最近100次解析时间
    };
    
    // 记录解析事件
    void recordResolution(const std::string& hostname, 
                         double duration, 
                         bool success);
    
    // 记录错误
    void recordError(const std::string& errorType);
    
    // 获取指标
    Metrics getMetrics() const;
    
    // 重置统计
    void reset();
    
    // 导出报告
    std::string generateReport() const;
    
private:
    mutable std::mutex mutex_;
    Metrics metrics_;
    
    // 计算百分位数
    double calculatePercentile(const std::vector<double>& data, double percentile);
};
```

#### 5.1 Kotlin监控接口
```kotlin
// 文件: jni/MMDNSMonitor.kt
data class DNSMetrics(
    val totalRequests: Long,
    val successfulRequests: Long,
    val failedRequests: Long,
    val cachedRequests: Long,
    val avgResolutionTime: Double,
    val cacheHitRate: Double,
    val memoryUsage: Long,
    val activeThreads: Int,
    val queuedTasks: Int
)

interface MetricsListener {
    fun onMetricsUpdate(metrics: DNSMetrics)
}

fun MMDNSManager.enableMonitoring(
    interval: Long = 5000,  // 5秒更新一次
    listener: MetricsListener
) {
    // 启动定期监控
}
```

---

## 🏗️ 项目结构规划

### 目录结构
```
Thread-P2P-Module/
├── cpp/dns/
│   ├── include/
│   │   ├── [现有头文件...]
│   │   ├── MMDNSHttpClient.h           # 新增：HTTP客户端
│   │   ├── MMDNSDualStackOptimizer.h   # 新增：双栈优化器
│   │   ├── MMDNSObjectPool.h           # 新增：对象池
│   │   ├── MMDNSConnectionPool.h       # 新增：连接池
│   │   └── MMDNSMonitor.h              # 新增：性能监控
│   │
│   ├── src/
│   │   ├── [现有实现文件...]
│   │   ├── MMDNSHttpClient.cpp         # 新增
│   │   ├── MMDNSDualStackOptimizer.cpp # 新增
│   │   ├── MMDNSObjectPool.cpp         # 新增
│   │   ├── MMDNSConnectionPool.cpp     # 新增
│   │   └── MMDNSMonitor.cpp            # 新增
│   │
│   ├── jni/
│   │   ├── mmdns_jni.cpp               # 现有JNI绑定
│   │   ├── MMDNSManager.kt             # 新增：Kotlin主接口
│   │   ├── MMDNSConfig.kt              # 新增：配置DSL
│   │   ├── MMDNSMonitor.kt             # 新增：监控接口
│   │   └── MMDNSExtensions.kt          # 新增：Kotlin扩展
│   │
│   ├── example/
│   │   ├── test_dns.cpp                # 现有C++测试
│   │   ├── TestKotlinAPI.kt            # 新增：Kotlin测试
│   │   └── PerformanceTest.kt          # 新增：性能测试
│   │
│   └── docs/
│       ├── API.md                      # 新增：API文档
│       ├── PERFORMANCE.md              # 新增：性能指南
│       └── MIGRATION.md                # 新增：迁移指南
│
└── prebuilt/
    └── arm64-v8a/
        ├── include/                    # 现有头文件
        └── lib/                        # 现有库文件
            ├── libcurl.a
            ├── libssl.a
            ├── libcrypto.a
            └── libz.a
```

---

## 🔧 实施步骤

### 阶段1: HTTP客户端集成 (Week 1)

**优先级**: 🔴 高

1. **更新CMakeLists.txt**
   - 链接libcurl、libssl、libcrypto、libz
   - 添加prebuilt库路径
   - 配置编译选项

2. **实现MMDNSHttpClient**
   - 创建头文件和实现文件
   - 实现DoH请求发送
   - 实现JSON响应解析
   - 添加连接池管理

3. **集成到MMDNSServerHandle**
   - 修改[`MMDNSHttpServerHandle::sendDohRequest()`](Thread-P2P-Module/cpp/dns/src/MMDNSServerHandle.cpp:112)
   - 修改[`MMDNSHttpServerHandle::parseDohResponse()`](Thread-P2P-Module/cpp/dns/src/MMDNSServerHandle.cpp:129)
   - 添加错误处理和重试机制

4. **测试验证**
   - 单元测试
   - 集成测试
   - 性能测试

---

### 阶段2: Kotlin接口层 (Week 1-2)

**优先级**: 🔴 高

1. **创建Kotlin核心接口**
   - MMDNSManager.kt - 主要管理类
   - DNSCallback接口
   - 数据类和枚举

2. **实现JNI增强**
   - 完善回调机制
   - 添加异常处理
   - 实现线程安全

3. **创建Kotlin DSL**
   - 配置DSL
   - 扩展函数
   - 协程支持

4. **文档和示例**
   - API文档
   - 使用示例
   - 最佳实践

---

### 阶段3: IPv6增强 (Week 2)

**优先级**: 🟡 中

1. **IPv6模型扩展**
   - 扩展MMDNSIPModel
   - 添加IPv6检测和验证
   - 实现IPv6特定功能

2. **双栈优化器**
   - 实现Happy Eyeballs算法
   - 并行连接测试
   - 智能IP选择

3. **DNS解析增强**
   - 同时查询A和AAAA记录
   - 优化解析策略
   - 添加IPv6测速

4. **测试**
   - IPv6环境测试
   - 双栈环境测试
   - 性能对比

---

### 阶段4: 性能优化 (Week 2-3)

**优先级**: 🟡 中

1. **对象池实现**
   - 通用对象池模板
   - 特化常用对象
   - 性能调优

2. **连接池实现**
   - HTTP连接池
   - Socket连接池
   - 生命周期管理

3. **内存优化**
   - 减少内存分配
   - 优化缓存策略
   - 内存泄漏检测

4. **性能测试**
   - 基准测试
   - 压力测试
   - 对比分析

---

### 阶段5: 监控系统 (Week 3)

**优先级**: 🟢 中低

1. **C++监控实现**
   - MMDNSMonitor类
   - 指标收集
   - 统计计算

2. **Kotlin监控接口**
   - Kotlin数据类
   - 监控回调
   - 报告生成

3. **可视化**
   - 日志输出
   - 性能报告
   - 实时监控

4. **集成测试**
   - 监控准确性验证
   - 性能影响评估

---

## 📊 技术方案细节

### HTTP DNS (DoH) 实现

#### 请求格式
```
GET /dns-query?name=example.com&type=A HTTP/1.1
Host: dns.google
Accept: application/dns-json
```

#### 响应格式 (JSON)
```json
{
  "Status": 0,
  "TC": false,
  "RD": true,
  "RA": true,
  "AD": false,
  "CD": false,
  "Question": [
    {
      "name": "example.com.",
      "type": 1
    }
  ],
  "Answer": [
    {
      "name": "example.com.",
      "type": 1,
      "TTL": 3600,
      "data": "93.184.216.34"
    }
  ]
}
```

#### libcurl使用示例
```cpp
std::string MMDNSHttpClient::sendDohRequest(
    const std::string& url,
    const std::string& hostname,
    const std::string& recordType) {
    
    CURL* curl = acquireConnection();
    if (!curl) return "";
    
    std::string fullUrl = url + "?name=" + hostname + "&type=" + recordType;
    std::string response;
    
    curl_easy_setopt(curl, CURLOPT_URL, fullUrl.c_str());
    curl_easy_setopt(curl, CURLOPT_WRITEFUNCTION, writeCallback);
    curl_easy_setopt(curl, CURLOPT_WRITEDATA, &response);
    curl_easy_setopt(curl, CURLOPT_TIMEOUT, 5L);
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 1L);
    curl_easy_setopt(curl, CURLOPT_SSL_VERIFYHOST, 2L);
    
    struct curl_slist* headers = nullptr;
    headers = curl_slist_append(headers, "Accept: application/dns-json");
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
    
    CURLcode res = curl_easy_perform(curl);
    
    curl_slist_free_all(headers);
    releaseConnection(curl);
    
    return (res == CURLE_OK) ? response : "";
}
```

---

### 对象池实现

```cpp
template<typename T>
class ObjectPool {
public:
    explicit ObjectPool(size_t initialSize, size_t maxSize)
        : maxSize_(maxSize) {
        for (size_t i = 0; i < initialSize; ++i) {
            pool_.push_back(createObject());
        }
    }
    
    std::shared_ptr<T> acquire() {
        std::lock_guard<std::mutex> lock(mutex_);
        
        if (!pool_.empty()) {
            auto obj = pool_.back();
            pool_.pop_back();
            return obj;
        }
        
        // 池已空，创建新对象
        if (totalCreated_ < maxSize_) {
            totalCreated_++;
            return createObject();
        }
        
        // 达到最大限制，等待或返回nullptr
        return nullptr;
    }
    
    void release(std::shared_ptr<T> obj) {
        if (!obj) return;
        
        std::lock_guard<std::mutex> lock(mutex_);
        
        // 重置对象状态
        resetObject(obj);
        
        if (pool_.size() < maxSize_) {
            pool_.push_back(obj);
        }
    }
    
private:
    std::vector<std::shared_ptr<T>> pool_;
    std::mutex mutex_;
    size_t maxSize_;
    size_t totalCreated_ = 0;
    
    std::shared_ptr<T> createObject() {
        return std::make_shared<T>();
    }
    
    void resetObject(std::shared_ptr<T>& obj) {
        // 子类特化实现
    }
};
```

---

### Kotlin DSL使用示例

```kotlin
// 初始化
val dnsManager = MMDNSManager.getInstance().apply {
    init(context)
    configure {
        dohServer = "https://cloudflare-dns.com/dns-query"
        enableSystemDNS = true
        enableHttpDNS = true
        cacheExpireTime = 3600
        threadCount = 4
        logLevel = LogLevel.DEBUG
    }
}

// 同步解析
val ip = dnsManager.resolveHost("www.google.com")

// 异步解析
dnsManager.resolveHostAsync("www.github.com") { ip, success ->
    if (success) {
        println("解析成功: $ip")
    }
}

// 协程支持
lifecycleScope.launch {
    val ip = dnsManager.resolveHost("www.example.com")
    // 使用IP
}

// 监控
dnsManager.enableMonitoring(interval = 5000) { metrics ->
    println("DNS请求: ${metrics.totalRequests}")
    println("缓存命中率: ${metrics.cacheHitRate}%")
    println("平均解析时间: ${metrics.avgResolutionTime}ms")
}
```

---

## 📈 性能目标

### 解析性能
- **缓存命中**: < 1ms
- **系统DNS**: < 100ms
- **HTTP DNS**: < 200ms
- **智能测速**: < 500ms

### 资源使用
- **内存占用**: < 10MB
- **线程数**: 4-8
- **CPU使用**: < 5%

### 可靠性
- **成功率**: > 99%
- **缓存命中率**: > 80%
- **并发支持**: > 1000 QPS

---

## 🧪 测试策略

### 单元测试
- HTTP客户端测试
- 对象池测试
- IPv6功能测试
- 监控系统测试

### 集成测试
- 端到端DNS解析
- 多线程并发测试
- 缓存一致性测试
- 错误处理测试

### 性能测试
- 基准测试
- 压力测试
- 长时间稳定性测试
- 内存泄漏测试

### 兼容性测试
- Android版本兼容性 (API 21+)
- 网络环境测试 (WiFi/4G/5G)
- IPv4/IPv6环境测试

---

## 📝 文档计划

1. **API文档** (`docs/API.md`)
   - Kotlin API参考
   - C++ API参考
   - 使用示例

2. **性能指南** (`docs/PERFORMANCE.md`)
   - 性能优化建议
   - 最佳实践
   - 常见问题

3. **迁移指南** (`docs/MIGRATION.md`)
   - 从旧版本迁移
   - Breaking Changes
   - 兼容性说明

4. **架构文档** (`docs/ARCHITECTURE.md`)
   - 系统架构
   - 模块设计
   - 数据流

---

## 🔄 后续迭代

### V2.0 功能规划
- [ ] DNS缓存预热
- [ ] 智能DNS路由
- [ ] 多地域DNS服务器
- [ ] DNS污染检测和防护
- [ ] 自定义DNS服务器配置

### V3.0 功能规划
- [ ] DNS-over-TLS (DoT)
- [ ] DNS-over-QUIC (DoQ)
- [ ] DNSSEC验证
- [ ] 机器学习优化DNS选择
- [ ] 全链路加密

---

## 📅 时间线

| 阶段 | 功能 | 时间 | 状态 |
|------|------|------|------|
| 1 | HTTP客户端集成 | Week 1 | ⏳ 待开始 |
| 2 | Kotlin接口层 | Week 1-2 | ⏳ 待开始 |
| 3 | IPv6增强 | Week 2 | ⏳ 待开始 |
| 4 | 性能优化 | Week 2-3 | ⏳ 待开始 |
| 5 | 监控系统 | Week 3 | ⏳ 待开始 |
| 6 | 测试和文档 | Week 3-4 | ⏳ 待开始 |

**预计完成时间**: 4周

---

## ✅ 验收标准

### 功能完整性
- ✅ HTTP DNS (DoH) 完全可用
- ✅ Kotlin API完整实现
- ✅ IPv6双栈支持
- ✅ 对象池和连接池实现
- ✅ 性能监控系统运行

### 性能指标
- ✅ 解析时间符合目标
- ✅ 内存使用在限制内
- ✅ 并发性能达标
- ✅ 缓存命中率 > 80%

### 代码质量
- ✅ 单元测试覆盖率 > 80%
- ✅ 集成测试通过
- ✅ 性能测试通过
- ✅ 代码审查通过

### 文档完善
- ✅ API文档完整
- ✅ 使用示例齐全
- ✅ 性能指南详细
- ✅ 迁移指南清晰

---

## 🎓 学习资源

### DNS相关
- [RFC 8484 - DNS Queries over HTTPS (DoH)](https://tools.ietf.org/html/rfc8484)
- [RFC 8305 - Happy Eyeballs Version 2](https://tools.ietf.org/html/rfc8305)
- [Google Public DNS](https://developers.google.com/speed/public-dns/docs/doh)

### libcurl
- [libcurl Documentation](https://curl.se/libcurl/)
- [libcurl Examples](https://curl.se/libcurl/c/example.html)

### Android开发
- [Android NDK Documentation](https://developer.android.com/ndk)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

---

**文档版本**: 1.0  
**最后更新**: 2025-01-25  
**维护者**: MMDNS Team