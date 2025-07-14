package com.example.module_fundamental.dialogTask.demo

import android.annotation.SuppressLint
import android.os.Bundle
import com.example.module_fundamental.dialogTask.BaseTaskActivity


class ProfileActivity: BaseTaskActivity() {
    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置初始Fragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(0x001, ProfileFragment())
                .commit()
        }
    }
}