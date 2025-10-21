package com.example.module_fundamental.cement2

import android.os.Handler
import androidx.recyclerview.widget.AdapterListUpdateCallback
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil

/**
 *
 * 异步Cement Adapter
 * 使用AsyncListDiffer实现异步Diff计算
 */
open class AsyncCementAdapter(diffHandler: Handler) : BaseCementAdapter() {
    
    /**
     * DiffUtil的ItemCallback
     */
    private val itemCallback = object : DiffUtil.ItemCallback<AsyncCementModel<*, *>>() {
        
        override fun areItemsTheSame(
            oldItem: AsyncCementModel<*, *>,
            newItem: AsyncCementModel<*, *>
        ): Boolean {
            return oldItem::class == newItem::class && 
                   oldItem.isItemTheSame(newItem)
        }
        
        override fun areContentsTheSame(
            oldItem: AsyncCementModel<*, *>,
            newItem: AsyncCementModel<*, *>
        ): Boolean {
            return oldItem::class == newItem::class && 
                   oldItem.isContentTheSame(newItem)
        }
        
        override fun getChangePayload(
            oldItem: AsyncCementModel<*, *>,
            newItem: AsyncCementModel<*, *>
        ): Any {
            return DiffPayload(oldItem)
        }
    }
    
    /**
     * AsyncListDiffer实例
     */
    private val asyncListDiffer = AsyncListDiffer(
        AdapterListUpdateCallback(this),
        AsyncDifferConfig.Builder(itemCallback)
            .setBackgroundThreadExecutor(HandlerExecutor(diffHandler))
            .build()
    )
    
    override fun getCurrentModels(): List<CementModel<*>> {
        @Suppress("UNCHECKED_CAST")
        return asyncListDiffer.currentList as List<CementModel<*>>
    }
    
    override val diffPayloadsEnabled: Boolean = true
    
    /**
     * 提交新的Model列表
     * @param models 新的Model列表
     * @param callback 提交完成回调
     */
    @JvmOverloads
    fun submitModels(
        models: List<AsyncCementModel<*, *>>,
        callback: (() -> Unit)? = null
    ) {
        viewHolderFactory.register(models)
        asyncListDiffer.submitList(models) {
            callback?.invoke()
        }
    }
}