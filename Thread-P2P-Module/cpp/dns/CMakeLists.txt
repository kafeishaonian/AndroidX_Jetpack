cmake_minimum_required(VERSION 3.10)
project(mmdns)

# 设置C++标准
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# 设置编译选项
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -Wextra")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -g -O0")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O2 -DNDEBUG")

# Prebuilt库路径
set(PREBUILT_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../prebuilt/arm64-v8a)

# 包含目录
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${PREBUILT_DIR}/include
)

# 收集所有源文件
set(MMDNS_SOURCES
    # 基础类
    src/MMDNSCommon.cpp
    src/MMDNSIPModel.cpp
    src/MMDNSHostModel.cpp
    
    # 网络层
    src/MMDNSSocket.cpp
    src/MMPing.cpp
    
    # 测速层
    src/MMDNSSpeedChecker.cpp
    
    # 任务系统
    src/MMDNSServerTask.cpp
    
    # DNS处理器
    src/MMDNSServerHandle.cpp
    
    # 管理器
    src/MMDNSHostManager.cpp
    src/MMDNSDataCache.cpp
    
    # 核心服务
    src/MMDNSServer.cpp
    
    # 入口类
    src/MMDNSEntrance.cpp
    
    # HTTP客户端
    src/MMDNSHttpClient.cpp
    
    # 双栈优化器
    src/MMDNSDualStackOptimizer.cpp
    
    # 连接池
    src/MMDNSConnectionPool.cpp
    
    # 性能监控
    src/MMDNSMonitor.cpp
)

# 收集所有头文件
set(MMDNS_HEADERS
    include/MMDNSCommon.h
    include/MMDNSBlockingQueue.h
    include/MMDNSIPModel.h
    include/MMDNSHostModel.h
    include/MMDNSServerTask.h
    include/MMDNSSocket.h
    include/MMPing.h
    include/MMDNSSpeedChecker.h
    include/MMDNSServerHandle.h
    include/MMDNSHostManager.h
    include/MMDNSDataCache.h
    include/MMDNSServer.h
    include/MMDNSEntrance.h
    include/MMDNSHttpClient.h
    include/MMDNSDualStackOptimizer.h
    include/MMDNSObjectPool.h
    include/MMDNSConnectionPool.h
    include/MMDNSMonitor.h
)

# 创建静态库
add_library(mmdns_static STATIC ${MMDNS_SOURCES} ${MMDNS_HEADERS})
set_target_properties(mmdns_static PROPERTIES OUTPUT_NAME mmdns)

# 创建共享库
add_library(mmdns SHARED ${MMDNS_SOURCES} ${MMDNS_HEADERS})

# 链接prebuilt库
target_link_libraries(mmdns
    log          # Android日志
    pthread      # 线程支持
    ${PREBUILT_DIR}/lib/libcurl.a
    ${PREBUILT_DIR}/lib/libssl.a
    ${PREBUILT_DIR}/lib/libcrypto.a
    ${PREBUILT_DIR}/lib/libz.a
)

target_link_libraries(mmdns_static
    log
    pthread
    ${PREBUILT_DIR}/lib/libcurl.a
    ${PREBUILT_DIR}/lib/libssl.a
    ${PREBUILT_DIR}/lib/libcrypto.a
    ${PREBUILT_DIR}/lib/libz.a
)

# 设置目标属性
set_target_properties(mmdns PROPERTIES
    VERSION 1.0.0
    SOVERSION 1
    PUBLIC_HEADER "${MMDNS_HEADERS}"
)

# 安装规则
install(TARGETS mmdns mmdns_static
    LIBRARY DESTINATION lib
    ARCHIVE DESTINATION lib
    PUBLIC_HEADER DESTINATION include/mmdns
)

# 编译选项
target_compile_options(mmdns PRIVATE
    -fvisibility=hidden
    -fno-rtti
    -fno-exceptions
)

target_compile_options(mmdns_static PRIVATE
    -fvisibility=hidden
    -fno-rtti
    -fno-exceptions
)

# 打印配置信息
message(STATUS "MMDNS Library Configuration:")
message(STATUS "  C++ Standard: ${CMAKE_CXX_STANDARD}")
message(STATUS "  Build Type: ${CMAKE_BUILD_TYPE}")
message(STATUS "  Source Files: ${MMDNS_SOURCES}")
message(STATUS "  Header Files: ${MMDNS_HEADERS}")