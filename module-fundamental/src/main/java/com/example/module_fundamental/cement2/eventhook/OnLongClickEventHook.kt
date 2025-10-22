package com.example.module_fundamental.cement2.eventhook

import android.view.View
import com.example.module_fundamental.cement2.CementAdapter
import com.example.module_fundamental.cement2.CementModel
import com.example.module_fundamental.cement2.CementViewHolder

/**
 * 长按事件钩子
 * 用于为ViewHolder添加长按事件监听
 * 
 * @param VH ViewHolder类型
 */
abstract class OnLongClickEventHook<VH : CementViewHolder>(
    clazz: Class<VH>
) : EventHook<VH>(clazz) {
    
    /**
     * 长按事件回调
     * 
     * @param view 被长按的View
     * @param viewHolder 对应的ViewHolder
     * @param position ViewHolder在Adapter中的位置
     * @param rawModel 对应位置的Model
     * @return true表示事件已消费，false表示事件未消费
     */
    abstract fun onLongClick(
        view: View,
        viewHolder: VH,
        position: Int,
        rawModel: CementModel<*>
    ): Boolean
    
    /**
     * 事件触发时的回调实现
     * 为View设置长按监听器
     */
    override fun onEvent(view: View, viewHolder: VH, adapter: CementAdapter) {
        view.setOnLongClickListener { v ->
            val position = viewHolder.adapterPosition
            val rawModel = adapter.getModel(position)
            
            // 确保位置有效且Model不为null
            if (position != -1 && rawModel != null) {
                onLongClick(v, viewHolder, position, rawModel)
            } else {
                false
            }
        }
    }
}