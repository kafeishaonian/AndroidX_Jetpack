package com.example.module_fundamental.cement2

/**
 * DiffUtil辅助接口
 * 用于定义列表项的比较逻辑
 */
interface IDiffUtilHelper<T> {
    /**
     * 判断两个项是否为同一项
     * @param item 要比较的项
     * @return true表示是同一项
     */
    fun isItemTheSame(item: T): Boolean
    
    /**
     * 判断两个项的内容是否相同
     * @param item 要比较的项
     * @return true表示内容相同
     */
    fun isContentTheSame(item: T): Boolean
}