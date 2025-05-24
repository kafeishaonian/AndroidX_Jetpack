package com.aj.mvvm.ui.callback;

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import java.util.Objects
import java.util.concurrent.atomic.AtomicInteger

open class ProtectedPeekLiveData<T> : LiveData<T> {
    constructor() : super()

    constructor(value: T) : super(value)

    companion object{
        private const val START_VERSION = -1
    }

    private val mCurrentVersion = AtomicInteger(START_VERSION)

    protected var isAllowNullValue: Boolean = false


    /**
     * TODO 当 liveData 用作 event 时，可使用该方法观察 "生命周期敏感" 非粘性消息
     * <p>
     * state 可变且私有，event 只读且公有，
     * state 倒灌应景，event 倒灌不符预期，
     * <p>
     * 如这么说无体会，详见《吃透 LiveData 本质，享用可靠消息鉴权机制》解析：
     * https://xiaozhuanlan.com/topic/6017825943
     *
     * @param owner    activity 传入 this，fragment 建议传入 getViewLifecycleOwner
     * @param observer observer
     */
    override fun observe(owner: LifecycleOwner, observer: Observer<in T>) {
        super.observe(owner, createObserverWrapper(observer, mCurrentVersion.get()))
    }

    /**
     * TODO 当 liveData 用作 event 时，可使用该方法观察 "生命周期不敏感" 非粘性消息
     */
    override fun observeForever(observer: Observer<in T>) {
        super.observeForever(createObserverWrapper(observer, mCurrentVersion.get()))
    }

    /**
     * TODO 当 liveData 用作 state 时，可使用该方法来观察 "生命周期敏感" 粘性消息
     *
     * @param owner    activity 传入 this，fragment 建议传入 getViewLifecycleOwner
     * @param observer observer
     */
    fun observeSticky(owner: LifecycleOwner, observe: Observer<in T>) {
        super.observe(owner, createObserverWrapper(observe, START_VERSION))
    }

    /**
     * TODO 当 liveData 用作 state 时，可使用该方法来观察 "生命周期不敏感" 粘性消息
     *
     */
    fun observeStickForever(observer: Observer<in T>) {
        super.observeForever(createObserverWrapper(observer, START_VERSION))
    }

    /**
     * 只需要重写setValue
     * postView 最终还是会经过这里
     */
    override fun setValue(value: T) {
        mCurrentVersion.getAndIncrement()
        super.setValue(value)
    }

    /**
     * 添加一个包装类，自己维护一个版本号判断，用于无需map帮助也能逐一判断消费情况
     * 重写equals方法与hashCode，用于手动removeObserver时，忽略版本号的比那花引起的变化
     */
    inner class ObserverWrapper<T>(private val observer: Observer<in T>, private val version: Int): Observer<T> {

        override fun onChanged(value: T) {
            if (this@ProtectedPeekLiveData.mCurrentVersion.get() > version && (value != null || isAllowNullValue)) {
                observer.onChanged(value)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }

            if (other == null || javaClass != other.javaClass) {
                return false
            }

            val that = other as ObserverWrapper<*>
            return Objects.equals(observer, that.observer)
        }

        override fun hashCode(): Int {
            return Objects.hash(observer)
        }

    }

    /**
     * 通过ObserveForever Observe, 需记得remove, 不然存在LiveData内存泄漏隐患
     * 保险做法是， 在页面onDestroy环节安排removeObserver代码，
     *
     * @param observer observeForever 注册的 observer，或 observe 注册的 observerWrapper
     */
    override fun removeObserver(observer: Observer<in T>) {
        if (observer.javaClass.isAssignableFrom(ObserverWrapper::class.java)) {
            super.removeObserver(observer)
        } else {
            super.removeObserver(createObserverWrapper(observer, START_VERSION))
        }
    }


    private fun createObserverWrapper(observer: Observer<in T>, version: Int): ObserverWrapper<in T> = ObserverWrapper(observer, version)
    /**
     * 手动将消息从内存中清空
     * 以免无消息随着SharedViewModel长时间驻留而导致内存溢出发生
     */
    fun clean(){
        super.setValue(null)
    }
}
