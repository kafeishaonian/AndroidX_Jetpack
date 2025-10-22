package com.example.module_fundamental.cement2.eventhook

import android.view.View
import com.example.module_fundamental.cement2.CementAdapter
import com.example.module_fundamental.cement2.CementModel
import com.example.module_fundamental.cement2.CementViewHolder

/**
 * 点击事件钩子
 * 用于为ViewHolder添加点击事件监听
 * 
 * @param VH ViewHolder类型
 */
abstract class OnClickEventHook<VH : CementViewHolder>(
    clazz: Class<VH>
) : EventHook<VH>(clazz) {
    
    /**
     * 点击事件回调
     * 
     * @param view 被点击的View
     * @param viewHolder 对应的ViewHolder
     * @param position ViewHolder在Adapter中的位置
     * @param rawModel 对应位置的Model
     */
    abstract fun onClick(
        view: View,
        viewHolder: VH,
        position: Int,
        rawModel: CementModel<*>
    )
    
    /**
     * 事件触发时的回调实现
     * 为View设置点击监听器
     */
    override fun onEvent(view: View, viewHolder: VH, adapter: CementAdapter) {
        view.setOnClickListener { v ->
            val position = viewHolder.adapterPosition
            val rawModel = adapter.getModel(position)
            
            // 确保位置有效且Model不为null
            if (position != -1 && rawModel != null) {
                onClick(v, viewHolder, position, rawModel)
            }
        }
    }
}