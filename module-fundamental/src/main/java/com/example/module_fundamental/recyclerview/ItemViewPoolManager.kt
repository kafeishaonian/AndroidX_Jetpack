package com.example.module_fundamental.recyclerview

import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 负责管理各类型 BaseItemView 的缓存复用。
 * 支持全局共用、最大缓存限制、复用统计。
 * 支持弱引用缓存 + 生命周期自动清理
 */
class ItemViewPoolManager {

    private val poolMap = ConcurrentHashMap<Class<out BaseItemView<*>>, ArrayDeque<WeakReference<BaseItemView<*>>>>()

    private val maxCacheSize = ConcurrentHashMap<Class<out BaseItemView<*>>, Int>()

    private val reuseCount = AtomicInteger(0)

    private val createCount = AtomicInteger(0)

    private var lifecycleOwnerRef: WeakReference<LifecycleOwner>? = null

    /**
     * 从缓存池获取 ItemView（如果无可用则创建新的）
     */
    fun obtain(clazz: Class<out BaseItemView<*>>, parent: ViewGroup): BaseItemView<*> {
        val pool = poolMap.getOrPut(clazz) { ArrayDeque() }

        while (pool.isNotEmpty()) {
            val ref = pool.removeFirst()
            val cached = ref.get()
            if (cached != null) {
                reuseCount.incrementAndGet()
                return cached
            }
        }

        createCount.incrementAndGet()

        val constructors = clazz.declaredConstructors
        val targetCtor = constructors.find { it.parameterTypes.size == 1 }
            ?: throw IllegalStateException("No single-arg constructor found in ${clazz.name}")

        val fakeArg = when (val paramType = targetCtor.parameterTypes.first()) {
            Any::class.java -> Any()
            String::class.java -> ""
            Int::class.java, java.lang.Integer::class.java -> 0
            Boolean::class.java, java.lang.Boolean::class.java -> false
            else -> {
                // 尝试调用其无参构造
                try {
                    paramType.getDeclaredConstructor().newInstance()
                } catch (e: Exception) {
                    null
                }
            }
        }

        @Suppress("UNCHECKED_CAST")
        val itemView = targetCtor.newInstance(fakeArg) as BaseItemView<*>
        itemView.createView(parent)
        return itemView

    }

    /**
     * 回收 ItemView 进入缓存池（弱引用）
     */
    fun recycle(itemView: BaseItemView<*>) {
        val clazz = itemView::class.java
        val pool = poolMap.getOrPut(clazz) { ArrayDeque() }
        val maxSize = maxCacheSize[clazz] ?: 10
        if (pool.size < maxSize) {
            pool.addLast(WeakReference(itemView))
        }
    }

    /**
     * 设置某类型的最大缓存数量
     */
    fun setMaxCache(clazz: Class<out BaseItemView<*>>, size: Int) {
        maxCacheSize[clazz] = size
    }

    /**
     * 清空指定类型或全部缓存
     */
    fun clear(clazz: Class<out BaseItemView<*>>? = null) {
        if (clazz != null) poolMap.remove(clazz)
        else poolMap.clear()
    }

    /**
     * 自动绑定生命周期，onDestroy 时自动 clear()
     */
    fun autoClearOnLifecycle(owner: LifecycleOwner): ItemViewPoolManager {
        lifecycleOwnerRef = WeakReference(owner)
        owner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                clear()
            }
        })
        return this
    }

    /**
     * 获取统计信息
     */
    fun getStats(): String {
        val totalCreates = createCount.get()
        val totalReuses = reuseCount.get()
        val hitRate = if (totalCreates + totalReuses > 0)
            (totalReuses * 100 / (totalCreates + totalReuses))
        else 0
        return "ItemViewPool Stats -> created=$totalCreates, reused=$totalReuses, hitRate=${hitRate}%"
    }
}