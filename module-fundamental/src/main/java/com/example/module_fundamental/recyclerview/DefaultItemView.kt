package com.example.module_fundamental.recyclerview

import com.example.module_fundamental.R

class DefaultItemView: BaseItemView<Any>("") {
    override fun layoutResId(): Int {
        return R.layout.item_view_default
    }
}