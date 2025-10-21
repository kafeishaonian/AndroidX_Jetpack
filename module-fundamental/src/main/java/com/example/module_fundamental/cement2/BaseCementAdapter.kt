package com.example.module_fundamental.cement2

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.collection.LongSparseArray
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * Cement Adapter基类
 * 提供基础的Adapter功能和ViewHolder状态管理
 */
abstract class BaseCementAdapter : RecyclerView.Adapter<CementViewHolder>() {
    
    companion object {
        const val LOG_TAG = "CementAdapter"
        private const val SAVED_STATE_ARG_VIEW_HOLDERS = "saved_state_view_holders"
    }
    
    /**
     * ViewHolder工厂
     */
    internal val viewHolderFactory = ViewHolderFactory()
    
    /**
     * 已绑定的ViewHolder缓存
     */
    private val boundViewHolders = LongSparseArray<CementViewHolder>()
    
    /**
     * ViewHolder状态管理器
     */
    private var viewHolderState = ViewHolderState()
    
    /**
     * GridLayoutManager的跨度数
     */
    private var spanCount = 1
    
    /**
     * 是否已附加到RecyclerView
     */
    internal var isAttached = false
    
    /**
     * SpanSizeLookup懒加载
     */
    val spanSizeLookup: GridLayoutManager.SpanSizeLookup by lazy {
        object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val model = getModel(position)
                return model?.getSpanSize(spanCount, position, itemCount) ?: 1
            }
        }
    }
    
    /**
     * 是否启用Diff的Payload功能
     */
    protected open val diffPayloadsEnabled: Boolean = false
    
    /**
     * 获取当前的Model列表
     */
    abstract fun getCurrentModels(): List<CementModel<*>>
    
    init {
        setHasStableIds(true)
        spanSizeLookup.isSpanIndexCacheEnabled = true
    }
    
    /**
     * 设置GridLayoutManager的跨度数
     */
    fun setSpanCount(spanCount: Int) {
        this.spanCount = spanCount
    }
    
    /**
     * 获取所有Model列表
     */
    fun getModels(): List<CementModel<*>> = getCurrentModels()
    
    /**
     * 根据位置获取Model
     */
    fun getModel(position: Int): CementModel<*>? {
        return getCurrentModels().getOrNull(position)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CementViewHolder {
        return viewHolderFactory.create(viewType, parent)
    }
    
    override fun onBindViewHolder(holder: CementViewHolder, position: Int) {
        onBindViewHolder(holder, position, emptyList())
    }
    
    override fun onBindViewHolder(
        holder: CementViewHolder,
        position: Int,
        payloads: List<Any?>
    ) {
        val model = getModel(position) ?: return
        
        // 保存旧ViewHolder的状态
        boundViewHolders.get(holder.itemId)?.let { oldHolder ->
            viewHolderState.save(oldHolder)
        }
        
        // 提取previous model（如果启用了diff payloads）
        val previous = if (diffPayloadsEnabled) {
            DiffPayload.extract(payloads)
        } else {
            null
        }
        
        // 绑定数据
        holder.bind(model, previous, payloads)
        
        // 恢复状态
        viewHolderState.restore(holder)
        
        // 缓存ViewHolder
        boundViewHolders.put(holder.itemId, holder)
    }
    
    override fun onViewRecycled(holder: CementViewHolder) {
        viewHolderState.save(holder)
        boundViewHolders.remove(holder.itemId)
        holder.unbind()
    }
    
    override fun onViewAttachedToWindow(holder: CementViewHolder) {
        holder.model?._attachedToWindow(holder)
    }
    
    override fun onViewDetachedFromWindow(holder: CementViewHolder) {
        holder.model?._detachedFromWindow(holder)
    }
    
    override fun getItemCount(): Int {
        return getCurrentModels().size
    }
    
    override fun getItemViewType(position: Int): Int {
        return getModel(position)?.viewType ?: -1
    }
    
    override fun getItemId(position: Int): Long {
        return getModel(position)?.id ?: -1
    }
    
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        isAttached = true
    }
    
    /**
     * 保存ViewHolder状态
     */
    fun onSaveInstanceState(outState: Bundle) {
        // 保存所有绑定的ViewHolder状态
        for (i in 0 until boundViewHolders.size()) {
            val key = boundViewHolders.keyAt(i)
            boundViewHolders.get(key)?.let { holder ->
                viewHolderState.save(holder)
            }
        }
        
        // 检查必须有稳定ID
        if (!viewHolderState.isEmpty() && !hasStableIds()) {
            throw IllegalStateException("Must have stable ids when saving view holder state")
        }
        
        outState.putParcelable(SAVED_STATE_ARG_VIEW_HOLDERS, viewHolderState)
    }
    
    /**
     * 恢复ViewHolder状态
     */
    fun onRestoreInstanceState(inState: Bundle?) {
        if (boundViewHolders.size() > 0) {
            throw IllegalStateException(
                "State cannot be restored once views have been bound. " +
                "It should be done before adding the adapter to the recycler view."
            )
        }
        
        val savedState = inState?.getParcelable<ViewHolderState>(SAVED_STATE_ARG_VIEW_HOLDERS)
        if (savedState != null) {
            viewHolderState = savedState
        } else {
            Log.w(LOG_TAG, "can not get save view holder state")
        }
    }
}