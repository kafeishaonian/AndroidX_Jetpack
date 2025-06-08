package com.aj.mvvm.utils

import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable

class ProxyDrawable: StateListDrawable() {

    private var originDrawable: Drawable? = null

    override fun addState(stateSet: IntArray?, drawable: Drawable?) {
        if (stateSet?.size == 1 && stateSet[0] == 0) {
            originDrawable = drawable
        }
        super.addState(stateSet, drawable)
    }

    fun getOriginDrawable(): Drawable? {
        return originDrawable
    }

}