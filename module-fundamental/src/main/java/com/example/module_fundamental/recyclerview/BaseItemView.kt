package com.example.module_fundamental.recyclerview

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

abstract class BaseItemView<T>(
    var data: T
) {
    lateinit var root: View
        protected set

    var adapterPosition: Int = RecyclerView.NO_POSITION
        internal set


    open fun createView(parent: ViewGroup) {
        root = LayoutInflater.from(parent.context).inflate(layoutResId(), parent, false)
        onViewCreated(root)
    }

    protected abstract fun layoutResId() : Int

    protected open fun onViewCreated(root: View) {}

    open fun bindData() {}

    open fun updateData(newData: T) {}
    open fun unbind() {}

    open fun onViewAttachedToWindow() {}
    open fun onViewDetachedFromWindow() {}

}