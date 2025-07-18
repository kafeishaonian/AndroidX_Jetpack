package com.example.module_fundamental.start_page

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

object PreLoadCenter {

    private var isInit = false

    private val dispatchScope = MainScope()

    // 存储，需要预加载的页面
    private val preLoadPage: MutableMap<String, WeakReference<IPreLoad>> = mutableMapOf()

    // 存储，需要预加载的页面对应的数据
    private val preLoadData: MutableMap<String, Any?> = mutableMapOf()

    fun init(application: Application) {
        isInit = true
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks{
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity is IPreLoad) {
                    try {
                        getOrWait(activity)
                    } catch (t: Throwable) {
                        log("getOrWait error: $t")
                    }
                }
            }

            override fun onActivityStarted(activity: Activity) {

            }

            override fun onActivityResumed(activity: Activity) {
            }

            override fun onActivityPaused(activity: Activity) {
            }

            override fun onActivityStopped(activity: Activity) {
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
            }

            override fun onActivityDestroyed(activity: Activity) {
                if (activity is IPreLoad) {
                    // 清除页面
                    removePreLoadPage(activity.tag())
                    // 清除数据
                    removePreLoadData(activity.tag())
                }
            }

        })
    }

    /**
     * 数据过来后调用，用于通知页面 或者 存储
     * @param tag 预加载的tag，用于寻找对应的页面
     * @param data 预加载的页面需要的数据]
     * 这个调用需要放在Main线程
     */
    fun notifyOrSave(tag: String, data: Any?) {
        if (!isInit) return
        if (data == null) return
        dispatchScope.launch {
            // 如果，此时已经有页面，则直接通知页面即可，不需要存储；
            // 此时：页面先于数据到达，已经注册了观测器，直接将数据通知到页面即可
            if (preLoadPage.containsKey(tag)) {
                preLoadPage[tag]?.get()?.let { preLoad -> preLoad.notify(data)
                    // 如果，页面是只需要获取一次数据，则通知后就移除
                    if (preLoad.oneShot()) {
                        removePreLoadPage(tag)
                    }
                }
                return@launch
            }

            // 否则，就记录下来，等待页面启动后找过来
            addPreLoadData(tag, data)
        }
    }


    /**
     * 页面过来后，去获取数据 或者 等待
     * @param preLoad 需要预加载的页面
     */
    private fun getOrWait(preLoad: IPreLoad) {
        // 如果，此时已经有数据，则直接获取即可
        // 此时：数据先于页面到达
        if (preLoadData.containsKey(preLoad.tag())) {
            preLoadData[preLoad.tag()]?.let {
                preLoad.notify(it)
            }
            // 如果，页面不是只获取一次数据，而是持续性观测，则添加到观测队列
            if (!preLoad.oneShot()) {
                addPreLoadPage(preLoad)
            }
            // 移除数据
            removePreLoadData(preLoad.tag())
        } else {
            // 数据尚未到达，就加入观测队列等待数据
            addPreLoadPage(preLoad)
        }
    }

    private fun addPreLoadPage(preLoad: IPreLoad) {
        preLoadPage[preLoad.tag()] = WeakReference(preLoad)
        log("put page by tag: [${preLoad.tag()} = $preLoad], after put size is: ${preLoadPage.size}")
    }

    private fun removePreLoadPage(tag: String) {
        if (preLoadPage.containsKey(tag)) {
            val removeObj = preLoadPage.remove(tag)
            log("remove page by tag: [$tag= ${removeObj?.get()}], after remove size is: ${preLoadPage.size}")
        }
    }

    private fun addPreLoadData(tag: String, data: Any?) {
        preLoadData[tag] = data
        log("put data by tag: [$tag = $data], after put size is: ${preLoadData.size}")
    }

    private fun removePreLoadData(tag: String) {
        if (preLoadData.containsKey(tag)) {
            val removeObj = preLoadData.remove(tag)
            log("remove data by tag: [$tag= $removeObj], after remove size is: ${preLoadData.size}")
        }
    }


    fun log(msg: String) {
        Log.d("PreLoadCenter", "[$msg]")
    }
}