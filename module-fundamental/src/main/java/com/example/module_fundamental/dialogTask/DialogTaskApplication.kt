package com.example.module_fundamental.dialogTask

import android.app.Application
import com.example.module_fundamental.dialogTask.ConfigService.updateTaskConfigs
import com.example.module_fundamental.dialogTask.demo.MainActivity
import com.example.module_fundamental.dialogTask.demo.ProfileFragment
import org.json.JSONObject


class DialogTaskApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        // 初始化核心组件
        PageTracker.init(this)
        TaskManager.init()

        // 加载默认配置
        loadDefaultTaskConfigs();

    }

    private fun loadDefaultTaskConfigs() {
        val defaultConfigs: MutableList<TaskConfig> = ArrayList()


        // 示例配置1：用户调查弹窗
        val surveyConfig = TaskConfig()
        surveyConfig.taskId = "USER_SURVEY_DIALOG"
        surveyConfig.priority = 6
        surveyConfig.type = Task.TYPE_DIALOG
        surveyConfig.requiredPage = MainActivity::class.java // 只在主页面显示
        surveyConfig.delayMillis = 3000
        surveyConfig.minShowDuration = 5000
        defaultConfigs.add(surveyConfig)


        // 示例配置2：VIP推广活动
        val vipConfig = TaskConfig()
        vipConfig.taskId = "VIP_PROMO_ACTIVITY"
        vipConfig.priority = 8
        vipConfig.type = Task.TYPE_ACTIVITY
        vipConfig.requiredPage = ProfileFragment::class.java // 只在个人资料页显示
        vipConfig.delayMillis = 5000
        vipConfig.minShowDuration = 8000
        vipConfig.maxShowCount = 3 // 最多显示3次
        defaultConfigs.add(vipConfig)


        // 更新配置
        updateTaskConfigs(defaultConfigs)
    }


    /**
     * 处理网络请求返回
     */
    fun onApiResponse(response: JSONObject) {
        val configs = parseConfigs(response)
        if (configs != null) {
            updateTaskConfigs(configs)
        }
    }

    private fun parseConfigs(json: JSONObject): List<TaskConfig> {
        val configs: List<TaskConfig> = ArrayList()
        // 实际解析逻辑...
        return configs
    }
}