package com.example.module_fundamental.cement2

import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Cement ViewHolder基类
 * 用于绑定Model和管理ViewHolder生命周期
 */
open class CementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    
    /**
     * 当前绑定的Model
     */
    var model: CementModel<*>? = null
    
    /**
     * 绑定数据到ViewHolder
     * @param model 要绑定的Model
     * @param previousModel 前一个Model（用于Diff）
     * @param payloads Diff的载荷列表
     */
    fun bind(
        model: CementModel<*>,
        previousModel: CementModel<*>?,
        payloads: List<Any?>
    ) {
        when {
            previousModel != null -> {
                model._bindData(this, previousModel)
            }
            payloads.isNotEmpty() -> {
                model._bindData(this, payloads)
            }
            else -> {
                model._bindData(this)
            }
        }
        this.model = model
    }
    
    /**
     * 解绑数据
     */
    fun unbind() {
        model?._unbind(this)
        model = null
    }
    
    /**
     * 是否应该保存ViewHolder的状态
     * @return true表示应该保存状态
     */
    open fun shouldSaveViewState(): Boolean {
        return model?.shouldSaveViewState() ?: false
    }
}