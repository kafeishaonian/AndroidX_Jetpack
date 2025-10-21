package com.example.module_fundamental.cement2

import android.os.Parcel
import android.os.Parcelable
import android.util.SparseArray
import android.view.View
import androidx.collection.LongSparseArray
import com.example.module_fundamental.cement2.R

/**
 * ViewHolder状态管理
 * 用于保存和恢复ViewHolder的视图状态
 */
class ViewHolderState(initialCapacity: Int = 10) : 
    LongSparseArray<ViewHolderState.ViewState>(initialCapacity), Parcelable {
    
    companion object CREATOR : Parcelable.Creator<ViewHolderState> {
        override fun createFromParcel(source: Parcel): ViewHolderState {
            val size = source.readInt()
            val state = ViewHolderState(size)
            for (i in 0 until size) {
                val key = source.readLong()
                val value = source.readParcelable<ViewState>(ViewState::class.java.classLoader)
                state.put(key, value)
            }
            return state
        }
        
        override fun newArray(size: Int): Array<ViewHolderState?> {
            return arrayOfNulls(size)
        }
    }
    
    /**
     * 保存ViewHolder的状态
     */
    fun save(holder: CementViewHolder?) {
        if (holder == null || !holder.shouldSaveViewState()) {
            return
        }
        
        var state = get(holder.itemId)
        if (state == null) {
            state = ViewState()
        }
        state.save(holder.itemView)
        put(holder.itemId, state)
    }
    
    /**
     * 恢复ViewHolder的状态
     */
    fun restore(holder: CementViewHolder?) {
        if (holder == null || !holder.shouldSaveViewState()) {
            return
        }
        
        val state = get(holder.itemId) ?: return
        state.restore(holder.itemView)
    }
    
    override fun describeContents(): Int = 0
    
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        val size = size()
        parcel.writeInt(size)
        for (i in 0 until size) {
            parcel.writeLong(keyAt(i))
            parcel.writeParcelable(valueAt(i), 0)
        }
    }
    
    /**
     * 视图状态
     * 存储单个View的层次结构状态
     */
    class ViewState : SparseArray<Parcelable>, Parcelable {
        
        companion object CREATOR : Parcelable.ClassLoaderCreator<ViewState> {
            override fun createFromParcel(source: Parcel?): ViewState? {
                return createFromParcel(source, null)
            }
            
            override fun createFromParcel(source: Parcel?, loader: ClassLoader?): ViewState? {
                if (source == null) return null
                
                val size = source.readInt()
                val keys = IntArray(size)
                source.readIntArray(keys)
                val values = source.readParcelableArray(loader) ?: return null
                
                return ViewState(size, keys, values)
            }
            
            override fun newArray(size: Int): Array<ViewState?> {
                return arrayOfNulls(size)
            }
        }
        
        constructor() : super(10)
        
        private constructor(
            size: Int,
            keys: IntArray? = null,
            values: Array<Parcelable?>? = null
        ) : super(size) {
            if (keys != null && values != null) {
                for (i in 0 until size) {
                    put(keys[i], values[i])
                }
            }
        }
        
        /**
         * 保存View的层次状态
         */
        fun save(view: View) {
            val originalId = view.id
            setIdIfNoneExists(view)
            view.saveHierarchyState(this)
            view.id = originalId
        }
        
        /**
         * 恢复View的层次状态
         */
        fun restore(view: View) {
            val originalId = view.id
            setIdIfNoneExists(view)
            view.restoreHierarchyState(this)
            view.id = originalId
        }
        
        /**
         * 如果View没有ID则设置一个默认ID
         */
        private fun setIdIfNoneExists(view: View) {
            if (view.id == View.NO_ID) {
                view.id = R.id.view_model_state_saving_id
            }
        }
        
        override fun describeContents(): Int = 0
        
        override fun writeToParcel(parcel: Parcel, flags: Int) {
            val size = size()
            val keys = IntArray(size)
            val values = arrayOfNulls<Parcelable>(size)
            
            for (i in 0 until size) {
                keys[i] = keyAt(i)
                values[i] = valueAt(i)
            }
            
            parcel.writeInt(size)
            parcel.writeIntArray(keys)
            parcel.writeParcelableArray(values, flags)
        }
    }
}