package com.example.module_fundamental.cement2

import android.content.Context
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * ViewHolder工厂类
 * 负责注册和创建ViewHolder
 */
class ViewHolderFactory {
    
    /**
     * 存储viewType与(layoutRes, creator)的映射
     */
    private val creatorSparseArray = SparseArray<Pair<Int, IViewHolderCreator<*>>>()
    
    /**
     * 注册单个Model
     * @param model 要注册的Model
     */
    fun register(model: CementModel<*>) {
        val viewType = model.viewType
        if (viewType == -1) {
            throw RuntimeException("illegal viewType=$viewType")
        }
        
        if (creatorSparseArray.get(viewType) == null) {
            creatorSparseArray.put(
                viewType,
                Pair(model.layoutRes, model.viewHolderCreator)
            )
        }
    }
    
    /**
     * 批量注册Model集合
     * @param models 要注册的Model集合
     */
    fun register(models: Collection<CementModel<*>>) {
        models.forEach { model ->
            register(model)
        }
    }
    
    /**
     * 根据viewType创建ViewHolder
     * @param viewType 视图类型
     * @param parent 父ViewGroup
     * @return 创建的ViewHolder
     */
    fun create(viewType: Int, parent: ViewGroup): CementViewHolder {
        val info = creatorSparseArray.get(viewType)
            ?: throw RuntimeException("cannot find viewHolderCreator for viewType=$viewType")
        
        return try {
            val (layoutRes, creator) = info
            val view = LayoutInflater.from(parent.context)
                .inflate(layoutRes, parent, false)
            creator.create(view)
        } catch (e: Exception) {
            val resourceName = try {
                parent.context.resources.getResourceName(info.first)
            } catch (ex: Exception) {
                "unknown"
            }
            throw RuntimeException(
                """
                cannot inflate view=$resourceName
                reason:${e.message}
                """.trimIndent(),
                e
            )
        }
    }
}