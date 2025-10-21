package com.example.module_fundamental.cement2

import kotlinx.coroutines.*

/**
 * Cement Model基类
 * 所有列表项Model的基类，支持DiffUtil比较
 */
abstract class CementModel<T : CementViewHolder> : IDiffUtilHelper<CementModel<*>> {
    
    companion object {
        /**
         * ID计数器，用于生成唯一ID
         */
        @Volatile
        private var idCounter = -2L
        
        /**
         * 哈希函数 - 64位长整型
         */
        private fun hashLong64Bit(value: Long): Long {
            var hash = value xor (value shl 21)
            hash = hash xor (hash ushr 35)
            return hash xor (hash shl 4)
        }
        
        /**
         * 哈希函数 - 整型
         */
        private fun hashInt(value: Int): Int {
            var hash = value xor (value shl 13)
            hash = hash xor (hash ushr 17)
            return hash xor (hash shl 5)
        }
        
        /**
         * 哈希函数 - 字符串64位
         */
        private fun hashString64Bit(str: CharSequence): Long {
            var result = -3750763034362895579L
            val len = str.length
            for (i in 0 until len) {
                result = (result xor str[i].code.toLong()) * 1099511628211L
            }
            return result
        }
    }
    
    /**
     * Model的唯一ID
     */
    private var _id: Long
    
    /**
     * 协程作用域（懒加载）
     */
    @Volatile
    private var _coroutineScope: CoroutineScope? = null
    
    /**
     * 布局资源ID
     */
    abstract val layoutRes: Int
    
    /**
     * ViewHolder创建器
     */
    abstract val viewHolderCreator: IViewHolderCreator<T>
    
    /**
     * 默认构造函数，自动生成ID
     */
    constructor() {
        synchronized(CementModel::class.java) {
            _id = idCounter
            idCounter--
        }
    }
    
    /**
     * 带ID的构造函数
     */
    protected constructor(id: Long) {
        _id = id
    }
    
    /**
     * 获取ViewType（基于layoutRes的哈希）
     */
    internal val viewType: Int
        get() = hashInt(layoutRes)
    
    /**
     * 获取唯一ID
     */
    val id: Long
        get() = _id
    
    /**
     * 设置ID（支持多种方式）
     */
    protected fun id(id: Long) {
        if (id != -1L) {
            _id = id
        }
    }
    
    protected fun id(vararg ids: Number) {
        var result = 0L
        for (num in ids) {
            result = 31 * result + hashLong64Bit(num.hashCode().toLong())
        }
        id(result)
    }
    
    protected fun id(id1: Long, id2: Long) {
        val result = hashLong64Bit(id1)
        id(31 * result + hashLong64Bit(id2))
    }
    
    protected fun id(key: CharSequence) {
        id(hashString64Bit(key))
    }
    
    protected fun id(key: CharSequence, vararg otherKeys: CharSequence) {
        var result = hashString64Bit(key)
        for (otherKey in otherKeys) {
            result = 31 * result + hashString64Bit(otherKey)
        }
        id(result)
    }
    
    protected fun id(key: CharSequence, id: Long) {
        val result = hashString64Bit(key)
        id(31 * result + hashLong64Bit(id))
    }
    
    /**
     * 获取协程作用域
     */
    @Synchronized
    fun getCoroutineScope(): CoroutineScope {
        val scope = _coroutineScope
        if (scope == null || !scope.isActive) {
            _coroutineScope = CoroutineScope(
                SupervisorJob() + Dispatchers.Main.immediate
            )
        }
        return _coroutineScope 
            ?: throw IllegalStateException("coroutineScope should not be null")
    }
    
    /**
     * 获取跨度大小（用于GridLayoutManager）
     */
    open fun getSpanSize(totalSpanCount: Int, position: Int, itemCount: Int): Int = 1
    
    /**
     * 是否应该保存ViewHolder状态
     */
    open fun shouldSaveViewState(): Boolean = false
    
    /**
     * 绑定数据到ViewHolder（内部调用）
     */
    internal fun _bindData(holder: CementViewHolder) {
        @Suppress("UNCHECKED_CAST")
        bindData(holder as T)
    }
    
    internal fun _bindData(holder: CementViewHolder, payloads: List<Any?>) {
        @Suppress("UNCHECKED_CAST")
        bindData(holder as T, payloads)
    }
    
    internal fun _bindData(holder: CementViewHolder, previous: CementModel<*>) {
        @Suppress("UNCHECKED_CAST")
        bindData(holder as T, previous)
    }
    
    /**
     * 绑定数据到ViewHolder
     */
    open fun bindData(holder: T) {}
    
    /**
     * 绑定数据到ViewHolder（带payloads）
     */
    open fun bindData(holder: T, payloads: List<Any?>) {
        bindData(holder)
    }
    
    /**
     * 绑定数据到ViewHolder（带previous model）
     */
    open fun bindData(holder: T, previous: CementModel<*>) {
        bindData(holder)
    }
    
    /**
     * 解绑ViewHolder（内部调用）
     */
    internal fun _unbind(holder: CementViewHolder) {
        @Suppress("UNCHECKED_CAST")
        unbind(holder as T)
    }
    
    /**
     * 解绑ViewHolder
     */
    open fun unbind(holder: T) {}
    
    /**
     * ViewHolder附加到窗口（内部调用）
     */
    internal fun _attachedToWindow(holder: CementViewHolder) {
        @Suppress("UNCHECKED_CAST")
        attachedToWindow(holder as T)
    }
    
    /**
     * ViewHolder附加到窗口
     */
    open fun attachedToWindow(holder: T) {}
    
    /**
     * ViewHolder从窗口分离（内部调用）
     */
    internal fun _detachedFromWindow(holder: CementViewHolder) {
        @Suppress("UNCHECKED_CAST")
        detachedFromWindow(holder as T)
        
        // 取消协程作用域
        synchronized(this) {
            _coroutineScope?.cancel()
        }
    }
    
    /**
     * ViewHolder从窗口分离
     */
    open fun detachedFromWindow(holder: T) {}
    
    /**
     * 判断是否为同一项（用于DiffUtil）
     */
    override fun isItemTheSame(item: CementModel<*>): Boolean {
        return id == item.id
    }
    
    /**
     * 判断内容是否相同（用于DiffUtil）
     */
    override fun isContentTheSame(item: CementModel<*>): Boolean {
        return true
    }
}