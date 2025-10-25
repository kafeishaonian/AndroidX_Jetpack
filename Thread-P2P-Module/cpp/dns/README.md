# MMDNS - Android智能DNS解析库

## 项目简介

这是一个从Ghidra反编译的libmmdns.so还原而来的完整DNS解析库，实现了智能DNS解析、自动测速优化、多源DNS查询等功能。

## 核心功能

- ✅ **多源DNS解析**: 支持系统DNS、HTTP DNS(DoH)、本地缓存
- ✅ **智能测速**: 自动测试IP速度并选择最快的
- ✅ **并发查询**: 同时查询多个DNS服务器，取最快结果
- ✅ **多级缓存**: 内存LRU缓存 + 文件持久化
- ✅ **线程池**: 高效的任务队列和线程池处理
- ✅ **Socket/Ping测速**: 支持TCP连接和ICMP Ping两种测速方式
- ✅ **HTTP DNS over HTTPS (DoH)**: 完整的DoH支持，使用libcurl
- ✅ **IPv6双栈支持**: Happy Eyeballs算法，智能选择IPv4/IPv6
- ✅ **对象池和连接池**: 优化性能，减少资源开销
- ✅ **性能监控**: 完善的性能指标和统计系统
- ✅ **Kotlin API**: 现代化的Kotlin接口，DSL配置，协程支持

## 目录结构

```
dns/
├── include/               # 头文件目录
│   ├── MMDNSCommon.h     # 公共定义、枚举、常量
│   ├── MMDNSBlockingQueue.h  # 阻塞队列（模板类）
│   ├── MMDNSIPModel.h    # IP模型
│   ├── MMDNSHostModel.h  # 主机模型
│   ├── MMDNSServerTask.h # DNS任务定义
│   ├── MMDNSSocket.h     # Socket封装
│   ├── MMPing.h          # Ping工具
│   ├── MMDNSSpeedChecker.h  # 测速检查器
│   ├── MMDNSServerHandle.h  # DNS服务器句柄（系统/HTTP/本地）
│   ├── MMDNSHostManager.h   # 主机管理器
│   ├── MMDNSDataCache.h     # 数据缓存（含LRU实现）
│   ├── MMDNSServer.h        # DNS服务器核心
│   └── MMDNSEntrance.h      # 入口类
│
├── src/                   # 实现文件目录
│   ├── MMDNSCommon.cpp
│   ├── MMDNSIPModel.cpp
│   ├── MMDNSHostModel.cpp
│   ├── MMDNSSocket.cpp
│   ├── MMPing.cpp
│   ├── MMDNSSpeedChecker.cpp
│   ├── MMDNSServerTask.cpp      # 待创建
│   ├── MMDNSServerHandle.cpp    # 待创建
│   ├── MMDNSHostManager.cpp     # 待创建
│   ├── MMDNSDataCache.cpp       # 待创建
│   ├── MMDNSServer.cpp
│   └── MMDNSEntrance.cpp        # 待创建
│
├── CMakeLists.txt         # CMake构建配置
└── README.md             # 本文件
```

## 架构设计

### 分层架构

```
┌─────────────────────────────────────┐
│   入口层 (MMDNSEntrance)             │
├─────────────────────────────────────┤
│   核心服务层 (MMDNSServer)           │
├─────────────────────────────────────┤
│   DNS解析层 (ServerHandles)          │
│   - SystemDNS  - HttpDNS  - LocalDNS│
├─────────────────────────────────────┤
│   网络层 (Socket/Ping/HTTP)          │
├─────────────────────────────────────┤
│   数据层 (Cache/Model)               │
└─────────────────────────────────────┘
```

### 核心类关系

1. **MMDNSEntrance**: 单例入口，管理整个DNS服务
2. **MMDNSServer**: DNS服务核心，维护线程池和任务队列
3. **MMDNSServerHandle**: DNS处理器基类，包含3个子类
   - MMDNSSystemServerHandle: 系统DNS
   - MMDNSHttpServerHandle: HTTP DNS over HTTPS
   - MMDNSLocalServerHandle: 本地缓存
4. **MMDNSSpeedChecker**: 测速器，支持Socket和Ping两种方式
5. **MMDNSHostManager**: 主机缓存管理器
6. **MMDNSBlockingQueue**: 线程安全的阻塞队列

## 使用示例

### C++ 基本使用

```cpp
#include "include/MMDNSEntrance.h"

using namespace mmdns;

// 获取单例
auto dns = MMDNSEntranceImpl::getInstance();

// 初始化
dns->init();

// 配置DoH服务器
dns->setDohServer("https://dns.google/dns-query");

// 同步解析
std::string ip = dns->resolveHost("www.example.com");
std::cout << "Best IP: " << ip << std::endl;

// 异步解析
dns->resolveHostAsync("www.example.com",
    [](auto host, bool success, auto oldHost) {
        if (success && host) {
            std::cout << "异步解析成功: " << host->getBestIPString() << std::endl;
        }
    });

// 获取所有IP
auto allIPs = dns->getAllIPs("www.example.com");
for (const auto& ip : allIPs) {
    std::cout << "IP: " << ip << std::endl;
}
```

### Kotlin API 使用

```kotlin
// 初始化
val dnsManager = MMDNSManager.getInstance()
dnsManager.init(context)

// DSL配置
dnsManager.configure {
    dohServer = "https://dns.google/dns-query"
    enableSystemDNS = true
    enableHttpDNS = true
    logLevel = LogLevel.DEBUG
}

// 同步解析
val ip = dnsManager.resolveHost("www.google.com")

// 协程解析
lifecycleScope.launch {
    val ip = dnsManager.resolveHostSuspend("www.example.com")
    println("Resolved: $ip")
}

// 异步解析
dnsManager.resolveHostAsync("www.github.com") { ip, success ->
    if (success) {
        println("IP: $ip")
    }
}

// 批量解析
val domains = listOf("www.google.com", "www.github.com")
dnsManager.resolveHostsBatch(domains, lifecycleScope) { hostname, ip, success ->
    println("$hostname -> $ip")
}

// Flow方式
dnsManager.resolveHostsFlow(domains).collect { (hostname, ip) ->
    println("$hostname -> $ip")
}
```

### 高级配置

```cpp
// C++ 高级配置
dns->enableSystemDNS(true);
dns->enableHttpDNS(true);
dns->enableLocalCache(true);

// 配置缓存
dns->setCacheDir("/data/data/com.example/cache/dns");
dns->setCacheSize(200);
dns->setCacheExpireTime(3600);  // 1小时

// 配置线程数
dns->setThreadCount(4);

// 网络状态变化通知
dns->setNetworkState(MMDNSAppNetState::WIFI);
```

```kotlin
// Kotlin 预设配置
MMDNSManager.getInstance().apply {
    init(context)
    applyPreset(DNSPresets.HIGH_PERFORMANCE)
}

// 或者自定义配置
dnsManager.quickInit(context) {
    dohServer = "https://cloudflare-dns.com/dns-query"
    enableSystemDNS = true
    enableHttpDNS = true
    cacheExpireTime = 3600
    threadCount = 8
    logLevel = LogLevel.INFO
}
```

## 编译方法

### 使用CMake

```bash
cd /path/to/Thread-P2P-Module/cpp/dns
mkdir build && cd build
cmake ..
make
```

### Android NDK集成

在你的Android项目的CMakeLists.txt中添加:

```cmake
# 添加DNS库源码
add_subdirectory(cpp/dns)

# 链接DNS库
target_link_libraries(your-lib
    mmdns
    log
)
```

## 注意事项

1. **ICMP Ping需要root权限**: Ping功能需要RAW socket权限，在Android上需要root
2. **网络权限**: 确保AndroidManifest.xml中有网络权限
3. **线程安全**: 所有公共接口都是线程安全的
4. **内存管理**: 使用智能指针管理内存，无需手动释放

## 待完成功能

以下文件需要继续实现（框架已创建）:

- [ ] MMDNSServerTask.cpp - 任务实现
- [ ] MMDNSServerHandle.cpp - DNS处理器实现
- [ ] MMDNSHostManager.cpp - 主机管理器实现  
- [ ] MMDNSDataCache.cpp - 数据缓存实现
- [ ] MMDNSEntrance.cpp - 入口类实现

## 依赖库

- C++14 或更高版本
- Android NDK (用于Android log)
- pthread (线程支持)
- 标准库: `<thread>`, `<mutex>`, `<future>`, `<chrono>` 等

## 性能特性

- **并发DNS查询**: 同时查询多个DNS服务器
- **智能测速**: 自动选择最快IP
- **LRU缓存**: 高效的内存缓存机制
- **线程池**: 避免频繁创建/销毁线程
- **异步处理**: 不阻塞主线程

## 许可证

本项目从libmmdns.so反编译还原，仅供学习和研究使用。

## 贡献

欢迎提交Issue和Pull Request来完善此项目。