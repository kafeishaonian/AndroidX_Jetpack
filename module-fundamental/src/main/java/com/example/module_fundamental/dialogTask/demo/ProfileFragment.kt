package com.example.module_fundamental.dialogTask.demo

import com.example.module_fundamental.dialogTask.BaseTaskFragment
import com.example.module_fundamental.dialogTask.TaskManager



class ProfileFragment: BaseTaskFragment() {

    override fun onResume() {
        super.onResume()
        // 尝试添加VIP推广活动
        if (activity != null) {
            TaskManager.get().addTask(
                "VIP_PROMO_ACTIVITY",
                requireActivity()
            )
        }
    }

}