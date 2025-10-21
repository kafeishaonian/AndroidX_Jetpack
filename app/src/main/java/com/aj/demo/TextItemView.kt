package com.aj.demo

import android.view.View
import android.widget.TextView
import com.example.module_fundamental.recyclerview.BaseItemView

class TextItemView(val name: String): BaseItemView<String>(name) {

    private var test: TextView? = null

    override fun layoutResId(): Int {
        return R.layout.item_view_text
    }

    override fun onViewCreated(root: View) {
        test = root.findViewById(R.id.text)
    }

    override fun bindData() {
        super.bindData()
        test?.text = name
    }
}