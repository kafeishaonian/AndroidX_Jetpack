package com.example.module_fundamental.dialogTask.demo

import android.content.Intent
import android.os.Bundle
import com.example.module_fundamental.dialogTask.BaseTaskActivity
import com.example.module_fundamental.dialogTask.TaskManager


class MainActivity: BaseTaskActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // 页面加载完成后，尝试添加符合条件的任务
        TaskManager.get().addTask(
            "USER_SURVEY_DIALOG",
            this
        )
    }

    fun openProfile() {
        // 模拟打开个人资料页面
        startActivity(Intent(this, ProfileActivity::class.java))
    }

}