package com.aj.demo

import android.view.View
import android.widget.TextView
import com.example.module_fundamental.cement2.CementModel
import com.example.module_fundamental.cement2.CementViewHolder
import com.example.module_fundamental.cement2.IViewHolderCreator

class TextItemView(val textStr: String) : CementModel<TextItemView.ViewHolder>() {


    override val layoutRes: Int = R.layout.item_view_text

    override val viewHolderCreator: IViewHolderCreator<ViewHolder> = object: IViewHolderCreator<ViewHolder>{
        override fun create(view: View): ViewHolder {
            return ViewHolder(view)
        }
    }

    override fun bindData(holder: ViewHolder) {
        super.bindData(holder)
        holder.onBind(textStr)
    }


    inner class ViewHolder(itemView: View) : CementViewHolder(itemView) {
        val textView = itemView.findViewById<TextView>(R.id.text)

        fun onBind(str: String){
            textView.text = str
        }
    }
}