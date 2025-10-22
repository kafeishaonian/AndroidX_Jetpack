package com.example.module_fundamental.cement2.eventhook

import android.view.View
import com.example.module_fundamental.cement2.CementAdapter
import com.example.module_fundamental.cement2.CementModel
import com.example.module_fundamental.cement2.CementViewHolder

/**
 * 事件钩子基类
 * 用于为特定类型的ViewHolder添加事件监听
 * 
 * @param VH ViewHolder类型
 */
abstract class EventHook<VH : CementViewHolder>(
    internal val clazz: Class<VH>
) {
    
    /**
     * 绑定事件到View
     * 在ViewHolder创建时调用，返回需要绑定事件的View
     * 
     * @param viewHolder 要绑定事件的ViewHolder
     * @return 要添加事件监听的View，如果返回null则不绑定
     */
    open fun onBind(viewHolder: VH): View? = null
    
    /**
     * 绑定事件到多个View
     * 在ViewHolder创建时调用，返回需要绑定事件的View列表
     * 
     * @param viewHolder 要绑定事件的ViewHolder
     * @return 要添加事件监听的View列表，如果返回null则不绑定
     */
    open fun onBindMany(viewHolder: VH): List<View>? = null
    
    /**
     * 事件触发时的回调
     * 子类需要实现此方法来处理具体的事件
     * 
     * @param view 触发事件的View
     * @param viewHolder 对应的ViewHolder
     * @param adapter 对应的Adapter
     */
    abstract fun onEvent(view: View, viewHolder: VH, adapter: CementAdapter)
    
    /**
     * 获取ViewHolder位置对应的原始Model
     * 
     * @param viewHolder ViewHolder
     * @param adapter Adapter
     * @return 对应位置的Model
     */
    protected fun getRawModel(viewHolder: VH, adapter: CementAdapter): CementModel<*>? {
        val position = viewHolder.adapterPosition
        return adapter.getModel(position)
    }
}