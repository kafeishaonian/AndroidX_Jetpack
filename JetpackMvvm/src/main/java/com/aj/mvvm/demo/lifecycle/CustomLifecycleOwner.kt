package com.aj.mvvm.demo.lifecycle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry

class CustomLifecycleOwner: LifecycleOwner {
    private lateinit var mRegistry: LifecycleRegistry

    fun init() {
        mRegistry = LifecycleRegistry(this)
        mRegistry.currentState = Lifecycle.State.CREATED
    }

    fun onStart() {
        mRegistry.currentState = Lifecycle.State.STARTED
    }

    fun onResume() {
        mRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun onPause() {
        mRegistry.currentState = Lifecycle.State.STARTED
    }

    fun onStop() {
        mRegistry.currentState = Lifecycle.State.CREATED
    }

    fun onDestroy() {
        mRegistry.currentState = Lifecycle.State.DESTROYED
    }

    override val lifecycle: Lifecycle = mRegistry
}