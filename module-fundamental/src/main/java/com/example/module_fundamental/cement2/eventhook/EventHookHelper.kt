package com.example.module_fundamental.cement2.eventhook

import android.view.View
import com.example.module_fundamental.cement2.CementAdapter
import com.example.module_fundamental.cement2.CementViewHolder

/**
 * 事件钩子辅助类
 * 负责管理和绑定EventHook到ViewHolder
 */
class EventHookHelper(private val cementAdapter: CementAdapter) {
    
    /**
     * 事件钩子列表
     */
    private val eventHooks = mutableListOf<EventHook<in CementViewHolder>>()
    
    /**
     * 标记是否已经开始绑定
     */
    private var isAfterBind = false
    
    /**
     * 添加事件钩子
     * 必须在绑定之前调用
     * 
     * @param eventHook 要添加的事件钩子
     */
    fun add(eventHook: EventHook<*>) {
        require(!isAfterBind) { "can not add event hook after bind" }
        
        @Suppress("UNCHECKED_CAST")
        eventHooks.add(eventHook as EventHook<in CementViewHolder>)
    }
    
    /**
     * 绑定事件钩子到ViewHolder
     * 
     * @param viewHolder 要绑定的ViewHolder
     */
    fun bind(viewHolder: CementViewHolder) {
        for (eventHook in eventHooks) {
            // 检查ViewHolder类型是否匹配
            if (eventHook.clazz.isInstance(viewHolder)) {
                // 类型转换
                val vh = eventHook.clazz.cast(viewHolder) ?: continue
                
                // 绑定单个View
                val view = eventHook.onBind(vh)
                if (view != null) {
                    attachToView(eventHook, vh, view)
                }
                
                // 绑定多个View
                val viewList = eventHook.onBindMany(vh)
                viewList?.forEach { v ->
                    if (v != null) {
                        attachToView(eventHook, vh, v)
                    }
                }
            }
        }
    }
    
    /**
     * 将事件钩子附加到指定View
     * 
     * @param eventHook 事件钩子
     * @param viewHolder ViewHolder
     * @param view 要附加的View
     */
    private fun attachToView(
        eventHook: EventHook<in CementViewHolder>,
        viewHolder: CementViewHolder,
        view: View
    ) {
        eventHook.onEvent(view, viewHolder, cementAdapter)
        isAfterBind = true
    }
}