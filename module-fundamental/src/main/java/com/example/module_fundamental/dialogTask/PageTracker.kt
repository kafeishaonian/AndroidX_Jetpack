package com.example.module_fundamental.dialogTask

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment

class PageTracker private constructor() : Application.ActivityLifecycleCallbacks{

    companion object {
        @Volatile
        private var instance: PageTracker? = null

        fun init(app: Application) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = PageTracker()
                        app.registerActivityLifecycleCallbacks(instance)
                    }
                }
            }
        }

        fun get(): PageTracker = instance ?: throw IllegalStateException("PageTracker not initialized")
    }

    private var currentActivity: Activity? = null
    private var currentFragment: Fragment? = null
    private val pageObservers = ArrayList<PageConditionObserver>()

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        notifyPageChanged()
    }

    override fun onActivityPaused(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    override fun onActivityStopped(activity: Activity) {
        // 不处理，因为onPaused已经处理
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    // 其他生命周期方法空实现...
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    fun setCurrentFragment(fragment: Fragment) {
        this.currentFragment = fragment
        notifyPageChanged()
    }

    fun clearCurrentFragment() {
        this.currentFragment = null
    }

    fun getCurrentActivity(): Activity? = currentActivity

    fun getCurrentFragment(): Fragment? = currentFragment

    fun isInPage(pageClass: Class<*>): Boolean {
        return (currentActivity != null && currentActivity!!::class.java == pageClass) ||
                (currentFragment != null && currentFragment!!::class.java == pageClass)
    }

    fun addPageObserver(observer: PageConditionObserver) {
        pageObservers.add(observer)
        // 立即检查是否满足条件
        observer.check(currentActivity, currentFragment)
    }

    fun removePageObserver(observer: PageConditionObserver) {
        pageObservers.remove(observer)
    }

    private fun notifyPageChanged() {
        val iterator = pageObservers.iterator()
        while (iterator.hasNext()) {
            val observer = iterator.next()
            observer.check(currentActivity, currentFragment)
            if (observer.isSatisfied()) {
                iterator.remove()
            }
        }
    }

    interface PageConditionObserver {
        fun check(activity: Activity?, fragment: Fragment?)
        fun isSatisfied(): Boolean
    }
}