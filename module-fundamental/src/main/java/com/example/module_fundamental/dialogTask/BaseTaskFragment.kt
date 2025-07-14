package com.example.module_fundamental.dialogTask

import androidx.fragment.app.Fragment

abstract class BaseTaskFragment: Fragment(){
    override fun onResume() {
        super.onResume()
        if (activity is BaseTaskActivity) {
            (activity as BaseTaskActivity).setCurrentFragment(this)
        }
    }

    override fun onPause() {
        super.onPause()
        PageTracker.get().clearCurrentFragment()
    }
}
