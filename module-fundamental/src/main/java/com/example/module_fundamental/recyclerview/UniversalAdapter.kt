package com.example.module_fundamental.recyclerview

import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.atomic.AtomicInteger

class UniversalAdapter(
    private val itemViewPoolManager: ItemViewPoolManager = ItemViewPoolManager()
) : RecyclerView.Adapter<UniversalAdapter.GenericVH>() {

    private val itemViews = mutableListOf<BaseItemView<*>>()
    private val viewTypeMap = mutableMapOf<Class<out BaseItemView<*>>, Int>()
    private val viewTypeCounter = AtomicInteger(0)

    private val clickListeners =
        mutableMapOf<Class<out BaseItemView<*>>, MutableMap<Int, (View, Any?, Int) -> Unit>>()

    fun addItemView(itemView: BaseItemView<*>) {
        itemViews.add(itemView)
        notifyItemInserted(itemViews.size - 1)
    }

    fun addItems(vararg views: BaseItemView<*>) {
        val start = itemViews.size
        itemViews.addAll(views)
        notifyItemRangeInserted(start, views.size)
    }

    fun addItems(views: List<BaseItemView<*>>) {
        val start = itemViews.size
        itemViews.addAll(views)
        Log.e("LogLogLog", "-----addItems------> ${views.size}。 start:= $start")
        notifyItemRangeInserted(start, views.size)
    }

    fun clear() {
        itemViews.forEach { it.unbind() }
        itemViews.clear()
        notifyDataSetChanged()
    }

    fun <T> updateItemAt(position: Int, newData: T) {
        val itemView = itemViews.getOrNull(position) ?: return
        (itemView as? BaseItemView<T>)?.updateData(newData)
        notifyItemChanged(position)
    }

    fun <T : BaseItemView<*>> addEventListener(
        itemViewClass: Class<T>,
        viewId: Int,
        handler: (View, Any?, Int) -> Unit
    ) {
        val map = clickListeners.getOrPut(itemViewClass) { mutableMapOf() }
        map[viewId] = handler
    }

    override fun getItemCount(): Int {
        Log.e("LogLogLog", "-----getItemCount------> ${itemViews.size}")
        return itemViews.size
    }

    override fun getItemViewType(position: Int): Int {
        val clazz = itemViews[position]::class.java
        Log.e("LogLogLog", "-----getItemViewType------> ${viewTypeMap.getOrPut(clazz) { viewTypeCounter.getAndIncrement() }}")
        return viewTypeMap.getOrPut(clazz) { viewTypeCounter.getAndIncrement() }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericVH {
        val clazz = viewTypeMap.entries.find { it.value == viewType }?.key ?: run {
            throw IllegalArgumentException("No view class found for type: $viewType")
        }
        val itemView = itemViewPoolManager.obtain(clazz, parent)
        Log.e("LogLogLog", "---------> $itemView")
        return GenericVH(itemView.root, itemView)
    }

    override fun onBindViewHolder(holder: GenericVH, position: Int) {
        val itemView = itemViews[position]
        holder.bind(itemView, position)
        bindClickListeners(holder.itemView, itemView, position)
    }

    override fun onViewRecycled(holder: GenericVH) {
        super.onViewRecycled(holder)
        val itemView = holder.itemViewInstance
        itemView.unbind()
        itemViewPoolManager.recycle(itemView)
    }

    override fun onViewAttachedToWindow(holder: GenericVH) {
        holder.itemViewInstance.onViewAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(holder: GenericVH) {
        holder.itemViewInstance.onViewDetachedFromWindow()
    }

    private fun bindClickListeners(root: View, itemView: BaseItemView<*>, position: Int) {
        val typeListeners = clickListeners[itemView::class.java] ?: return
        typeListeners.forEach { (viewId, handler) ->
            root.findViewById<View>(viewId)?.setOnClickListener { v ->
                handler.invoke(v, itemView.data, position)
            }
        }
    }


    inner class GenericVH(
        itemRoot: View,
        val itemViewInstance: BaseItemView<*>
    ) : RecyclerView.ViewHolder(itemRoot) {
        fun bind(itemView: BaseItemView<*>, position: Int) {
            itemView.adapterPosition = position
            itemView.bindData()
        }
    }
}