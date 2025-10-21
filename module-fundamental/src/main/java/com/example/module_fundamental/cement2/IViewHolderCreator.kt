package com.example.module_fundamental.cement2

import android.view.View

/**
 * ViewHolder创建器接口
 * 用于创建指定类型的ViewHolder
 */
interface IViewHolderCreator<VH : CementViewHolder> {
    /**
     * 根据视图创建ViewHolder
     * @param view 用于创建ViewHolder的视图
     * @return 创建的ViewHolder实例
     */
    fun create(view: View): VH
}