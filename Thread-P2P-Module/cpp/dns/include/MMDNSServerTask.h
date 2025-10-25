#pragma once

#include "MMDNSCommon.h"
#include "MMDNSHostModel.h"

namespace mmdns {

// 任务回调类型
using TaskCallback = std::function<void(std::shared_ptr<MMDNSHostModel>, bool, std::shared_ptr<MMDNSHostModel>)>;

class MMDNSServerTask {
public:
    MMDNSServerTask(
        DNSServerTaskType taskType,
        const std::string& hostname,
        TaskCallback callback = nullptr
    );
    
    virtual ~MMDNSServerTask() = default;
    
    // Getter
    DNSServerTaskType getTaskType() const { return taskType_; }
    std::string getHostname() const { return hostname_; }
    TaskCallback getCallback() const { return callback_; }
    long getCreateTime() const { return createTime_; }
    int getPriority() const { return priority_; }
    
    // Setter
    void setPriority(int priority) { priority_ = priority; }
    void setCallback(TaskCallback callback) { callback_ = callback; }
    
    // 执行任务（由子类实现具体逻辑）
    virtual void execute() = 0;
    
    // 比较运算符（用于优先级队列）
    bool operator<(const MMDNSServerTask& other) const {
        return priority_ < other.priority_;
    }
    
protected:
    DNSServerTaskType taskType_;
    std::string hostname_;
    TaskCallback callback_;
    long createTime_;
    int priority_;  // 优先级，值越大优先级越高
};

// 解析主机任务
class ResolveHostTask : public MMDNSServerTask {
public:
    ResolveHostTask(const std::string& hostname, TaskCallback callback = nullptr);
    void execute() override;
};

// 速度检测任务
class SpeedCheckTask : public MMDNSServerTask {
public:
    SpeedCheckTask(
        const std::string& hostname,
        std::shared_ptr<MMDNSHostModel> hostModel,
        TaskCallback callback = nullptr
    );
    void execute() override;
    
private:
    std::shared_ptr<MMDNSHostModel> hostModel_;
};

// 缓存更新任务
class CacheUpdateTask : public MMDNSServerTask {
public:
    CacheUpdateTask(
        const std::string& hostname,
        std::shared_ptr<MMDNSHostModel> hostModel
    );
    void execute() override;
    
private:
    std::shared_ptr<MMDNSHostModel> hostModel_;
};

} // namespace mmdns