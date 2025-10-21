package com.example.module_fundamental.cement2

import androidx.annotation.IntDef

/**
 * 加载更多Model
 * 用于列表底部的加载更多功能
 */
abstract class CementLoadMoreModel<VH : CementViewHolder>(
    var state: Int = COMPLETE,
    private val onLoadMoreListener: (() -> Unit)? = null
) : CementModel<VH>() {
    
    companion object {
        /**
         * 开始加载状态
         */
        const val START = 0
        
        /**
         * 加载完成状态
         */
        const val COMPLETE = 1
        
        /**
         * 加载失败状态
         */
        const val FAILED = 2
    }
    
    /**
     * 加载更多状态注解
     */
    @IntDef(START, COMPLETE, FAILED)
    @Retention(AnnotationRetention.SOURCE)
    annotation class LoadMoreState
    
    /**
     * 标记是否已触发加载更多
     */
    private var onLoadMoreTriggered = false
    
    /**
     * 加载开始时的处理
     */
    protected abstract fun onLoadMoreStart(holder: VH)
    
    /**
     * 加载完成时的处理
     */
    protected abstract fun onLoadMoreComplete(holder: VH)
    
    /**
     * 加载失败时的处理
     */
    protected abstract fun onLoadMoreFailed(holder: VH)
    
    /**
     * 绑定数据
     */
    override fun bindData(holder: VH) {
        when (state) {
            START -> onLoadMoreStart(holder)
            COMPLETE -> onLoadMoreComplete(holder)
            FAILED -> onLoadMoreFailed(holder)
        }
    }
    
    /**
     * 判断是否为同一项
     * 加载更多项始终视为同一项
     */
    override fun isItemTheSame(item: CementModel<*>): Boolean = true
    
    /**
     * 判断内容是否相同
     * 通过状态来判断
     */
    override fun isContentTheSame(item: CementModel<*>): Boolean {
        val otherState = (item as? CementLoadMoreModel<*>)?.state ?: -1
        return state == otherState
    }
    
    /**
     * 触发加载更多
     */
    fun triggerLoadMore() {
        // 如果已触发或正在加载，不重复触发
        if (onLoadMoreTriggered || state == START) {
            return
        }
        
        onLoadMoreTriggered = true
        onLoadMoreListener?.invoke()
    }
}