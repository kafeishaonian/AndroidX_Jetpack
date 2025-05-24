package com.aj.mvvm.demo.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.ProcessLifecycleOwner

class LifecycleDemoActivity : Activity() {

    val owner: CustomLifecycleOwner = CustomLifecycleOwner()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        owner.init()

        owner.lifecycle.addObserver(MyLifecycleObserver())
    }

    override fun onStart() {
        super.onStart()
        owner.onStart()
    }

    override fun onResume() {
        super.onResume()
        owner.onResume()
    }

    override fun onPause() {
        super.onPause()
        owner.onPause()
    }

    override fun onStop() {
        super.onStop()
        owner.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        owner.onDestroy()
    }
}

class MyApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(MyLifecycleObserver())
    }
}