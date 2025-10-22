package com.example.module_fundamental.cement2

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.example.module_fundamental.cement2.eventhook.EventHook
import com.example.module_fundamental.cement2.eventhook.EventHookHelper
import com.example.module_fundamental.cement2.eventhook.OnClickEventHook
import com.example.module_fundamental.cement2.eventhook.OnLongClickEventHook

/**
 * Cement同步Adapter
 * 支持增删改查、DiffUtil、EventHook等功能
 */
open class CementAdapter : BaseCementAdapter() {
    
    /**
     * Model列表（内部类，用于自动注册ViewHolder）
     */
    private val models = ModelList()
    
    /**
     * 是否启用Diff Payloads
     */
    private var enableDiffPayloads = false
    
    /**
     * 事件钩子辅助类
     */
    private val eventHookHelper by lazy { EventHookHelper(this) }
    
    /**
     * Item点击事件钩子
     */
    private var onItemClickEventHook: EventHook<CementViewHolder>? = null
    
    /**
     * Item点击监听器
     */
    private var onItemClickListener: OnItemClickListener? = null
    
    /**
     * Item长按事件钩子
     */
    private var onItemLongClickEventHook: EventHook<CementViewHolder>? = null
    
    /**
     * Item长按监听器
     */
    private var onItemLongClickListener: OnItemLongClickListener? = null
    
    /**
     * Item点击监听器接口
     */
    interface OnItemClickListener {
        fun onClick(
            itemView: View,
            viewHolder: CementViewHolder,
            position: Int,
            model: CementModel<*>
        )
    }
    
    /**
     * Item长按监听器接口
     */
    interface OnItemLongClickListener {
        fun onLongClick(
            itemView: View,
            viewHolder: CementViewHolder,
            position: Int,
            model: CementModel<*>
        ): Boolean
    }
    
    /**
     * Model列表内部类
     * 自动注册ViewHolder到工厂
     */
    private inner class ModelList : ArrayList<CementModel<*>>() {
        
        override fun add(element: CementModel<*>): Boolean {
            viewHolderFactory.register(element)
            return super.add(element)
        }
        
        override fun add(index: Int, element: CementModel<*>) {
            viewHolderFactory.register(element)
            super.add(index, element)
        }
        
        override fun addAll(elements: Collection<CementModel<*>>): Boolean {
            viewHolderFactory.register(elements)
            return super.addAll(elements)
        }
        
        override fun addAll(index: Int, elements: Collection<CementModel<*>>): Boolean {
            viewHolderFactory.register(elements)
            return super.addAll(index, elements)
        }
    }
    
    override fun getCurrentModels(): List<CementModel<*>> = models
    
    override val diffPayloadsEnabled: Boolean
        get() = enableDiffPayloads
    
    /**
     * 启用Diff Payloads功能
     */
    fun enableDiffPayloads() {
        enableDiffPayloads = true
    }
    
    /**
     * 判断是否包含某个Model
     */
    fun containsModel(model: CementModel<*>): Boolean {
        return models.indexOf(model) >= 0
    }
    
    /**
     * 获取Model的索引
     */
    protected fun indexOfModel(model: CementModel<*>): Int {
        return models.indexOf(model)
    }
    
    /**
     * 获取某个Model之后的所有Model（子列表）
     */
    private fun getAllModelSubListAfter(model: CementModel<*>?): MutableList<CementModel<*>> {
        val index = models.indexOf(model)
        if (index == -1) {
            return ArrayList()
        }
        return models.subList(index + 1, models.size)
    }
    
    /**
     * 获取某个Model之后的所有Model（新列表）
     */
    fun getAllModelListAfter(model: CementModel<*>?): List<CementModel<*>> {
        val index = models.indexOf(model)
        if (index == -1) {
            return emptyList()
        }
        return ArrayList(models.subList(index + 1, models.size))
    }
    
    /**
     * 获取两个Model之间的所有Model
     */
    fun getAllModelListBetween(
        start: CementModel<*>?,
        end: CementModel<*>?
    ): List<CementModel<*>> {
        val startIdx = models.indexOf(start)
        val endIdx = models.indexOf(end)
        val startIndex = if (startIdx == -1) 0 else startIdx + 1
        val endIndex = if (endIdx == -1) models.size else endIdx
        
        return if (startIndex > endIndex) {
            emptyList()
        } else {
            ArrayList(models.subList(startIndex, endIndex))
        }
    }
    
    /**
     * 添加Model
     */
    fun addModel(modelToAdd: CementModel<*>) {
        val initialSize = models.size
        models.add(modelToAdd)
        notifyItemInserted(initialSize)
    }
    
    /**
     * 在指定位置添加Model
     */
    fun addModel(index: Int, modelToAdd: CementModel<*>) {
        if (index > models.size || index < 0) {
            return
        }
        models.add(index, modelToAdd)
        notifyItemInserted(index)
    }
    
    /**
     * 添加多个Model（可变参数）
     */
    fun addModels(vararg modelsToAdd: CementModel<*>) {
        addModels(modelsToAdd.toList())
    }
    
    /**
     * 添加多个Model（集合）
     */
    fun addModels(modelsToAdd: Collection<CementModel<*>>) {
        val initialSize = models.size
        models.addAll(modelsToAdd)
        notifyItemRangeInserted(initialSize, modelsToAdd.size)
    }
    
    /**
     * 在某个Model之前插入
     */
    fun insertModelBefore(
        modelToInsert: CementModel<*>,
        modelToInsertBefore: CementModel<*>?
    ) {
        val targetIndex = models.indexOf(modelToInsertBefore)
        if (targetIndex == -1) {
            return
        }
        models.add(targetIndex, modelToInsert)
        notifyItemInserted(targetIndex)
    }
    
    /**
     * 在某个Model之前插入多个Model
     */
    fun insertModelsBefore(
        modelsToInsert: Collection<CementModel<*>>,
        modelToInsertBefore: CementModel<*>?
    ) {
        val targetIndex = models.indexOf(modelToInsertBefore)
        if (targetIndex == -1) {
            return
        }
        models.addAll(targetIndex, modelsToInsert)
        notifyItemRangeInserted(targetIndex, modelsToInsert.size)
    }
    
    /**
     * 在某个Model之后插入
     */
    fun insertModelAfter(
        modelToInsert: CementModel<*>,
        modelToInsertAfter: CementModel<*>?
    ) {
        val modelIndex = models.indexOf(modelToInsertAfter)
        if (modelIndex == -1) {
            return
        }
        val targetIndex = modelIndex + 1
        models.add(targetIndex, modelToInsert)
        notifyItemInserted(targetIndex)
    }
    
    /**
     * 在某个Model之后插入多个Model
     */
    fun insertModelsAfter(
        modelsToInsert: Collection<CementModel<*>>,
        modelToInsertAfter: CementModel<*>?
    ) {
        val modelIndex = models.indexOf(modelToInsertAfter)
        if (modelIndex == -1) {
            return
        }
        val targetIndex = modelIndex + 1
        models.addAll(targetIndex, modelsToInsert)
        notifyItemRangeInserted(targetIndex, modelsToInsert.size)
    }
    
    /**
     * 通知Model改变
     */
    @JvmOverloads
    fun notifyModelChanged(model: CementModel<*>, payload: Any? = null) {
        val index = models.indexOf(model)
        if (index != -1) {
            notifyItemChanged(index, payload)
        }
    }
    
    /**
     * 移除Model
     */
    fun removeModel(modelToRemove: CementModel<*>?) {
        val index = models.indexOf(modelToRemove)
        if (index >= 0 && index < models.size) {
            models.removeAt(index)
            notifyItemRemoved(index)
        }
    }
    
    /**
     * 移除所有Model
     */
    fun removeAllModels() {
        val initialSize = models.size
        models.clear()
        notifyItemRangeRemoved(0, initialSize)
    }
    
    /**
     * 移除某个Model之后的所有Model
     */
    fun removeAllAfterModel(model: CementModel<*>?) {
        val initialSize = models.size
        val modelsToRemove = getAllModelSubListAfter(model)
        val numModelsRemoved = modelsToRemove.size
        
        if (numModelsRemoved == 0) {
            return
        }
        
        modelsToRemove.clear()
        notifyItemRangeRemoved(initialSize - numModelsRemoved, numModelsRemoved)
    }
    
    /**
     * 替换所有Model（使用DiffUtil）
     */
    @JvmOverloads
    fun replaceAllModels(
        modelsToReplace: List<CementModel<*>>,
        detectMove: Boolean = true
    ) {
        if (models.size == 0) {
            addModels(modelsToReplace)
            return
        }
        
        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = models.size
            
            override fun getNewListSize() = modelsToReplace.size
            
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldModel = models.getOrNull(oldItemPosition)
                val newModel = modelsToReplace.getOrNull(newItemPosition)
                return oldModel != null && newModel != null &&
                        oldModel::class == newModel::class &&
                        oldModel.isItemTheSame(newModel)
            }
            
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                val oldModel = models.getOrNull(oldItemPosition)
                val newModel = modelsToReplace.getOrNull(newItemPosition)
                return oldModel != null && newModel != null &&
                        oldModel::class == newModel::class &&
                        oldModel.isContentTheSame(newModel)
            }
        }, detectMove)
        
        models.clear()
        models.addAll(modelsToReplace)
        result.dispatchUpdatesTo(this)
    }
    
    /**
     * 替换单个Model
     */
    fun replaceModel(modelToReplace: CementModel<*>, modelOrigin: CementModel<*>) {
        val targetIndex = models.indexOf(modelOrigin)
        if (targetIndex == -1) {
            return
        }
        
        models.add(targetIndex, modelToReplace)
        models.remove(modelOrigin)
        
        if (diffPayloadsEnabled) {
            notifyItemChanged(targetIndex, DiffPayload(modelOrigin))
        } else {
            notifyItemChanged(targetIndex)
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CementViewHolder {
        val holder = super.onCreateViewHolder(parent, viewType)
        eventHookHelper.bind(holder)
        return holder
    }
    
    /**
     * 添加事件钩子
     */
    fun addEventHook(eventHook: EventHook<*>) {
        if (isAttached) {
            Log.w(LOG_TAG, "addEventHook is called after adapter attached")
        }
        eventHookHelper.add(eventHook)
    }
    
    /**
     * 添加Item点击事件钩子
     */
    private fun addOnItemClickEventHook() {
        val hook = object : OnClickEventHook<CementViewHolder>(CementViewHolder::class.java) {
            override fun onClick(
                view: View,
                viewHolder: CementViewHolder,
                position: Int,
                rawModel: CementModel<*>
            ) {
                onItemClickListener?.onClick(view, viewHolder, position, rawModel)
            }
            
            override fun onBind(viewHolder: CementViewHolder): View? {
                return if (viewHolder.itemView.isClickable) {
                    viewHolder.itemView
                } else {
                    null
                }
            }
        }
        addEventHook(hook)
        onItemClickEventHook = hook
    }
    
    /**
     * 设置Item点击监听器
     */
    fun setOnItemClickListener(onItemClickListener: OnItemClickListener?) {
        require(!(isAttached && this.onItemClickEventHook == null && onItemClickListener != null)) {
            "setOnItemClickListener must be called before the RecyclerView#setAdapter"
        }
        
        if (!isAttached && this.onItemClickEventHook == null) {
            addOnItemClickEventHook()
        }
        
        this.onItemClickListener = onItemClickListener
    }
    
    /**
     * 添加Item长按事件钩子
     */
    private fun addOnItemLongClickEventHook() {
        val hook = object : OnLongClickEventHook<CementViewHolder>(CementViewHolder::class.java) {
            override fun onLongClick(
                view: View,
                viewHolder: CementViewHolder,
                position: Int,
                rawModel: CementModel<*>
            ): Boolean {
                return onItemLongClickListener?.onLongClick(
                    view, viewHolder, position, rawModel
                ) ?: false
            }
            
            override fun onBind(viewHolder: CementViewHolder): View? {
                return if (viewHolder.itemView.isClickable) {
                    viewHolder.itemView
                } else {
                    null
                }
            }
        }
        addEventHook(hook)
        onItemLongClickEventHook = hook
    }
    
    /**
     * 设置Item长按监听器
     */
    fun setOnItemLongClickListener(onItemLongClickListener: OnItemLongClickListener?) {
        require(!(isAttached && this.onItemLongClickEventHook == null && onItemLongClickListener != null)) {
            "setOnItemLongClickListener must be called before the RecyclerView#setAdapter"
        }
        
        if (!isAttached && this.onItemLongClickEventHook == null) {
            addOnItemLongClickEventHook()
        }
        
        this.onItemLongClickListener = onItemLongClickListener
    }
}