package com.aj.mvvm.ui.callback


open class PeekLiveData<T> : ProtectedPeekLiveData<T> {


    constructor(value: T) : super(value)

    constructor() : super()


    public override fun setValue(value: T) {
        super.setValue(value)
    }


    override fun postValue(value: T) {
        super.postValue(value)
    }



    inner class Builder<T> {

        private var isAllowNullValue: Boolean = false

        fun setAllowNullValue(allowNUllValue: Boolean) : Builder<T> {
            this.isAllowNullValue = allowNUllValue
            return this
        }

        fun create(): PeekLiveData<T> {
            val liveData: PeekLiveData<T> = PeekLiveData()
            liveData.isAllowNullValue = this.isAllowNullValue
            return liveData
        }
    }

}