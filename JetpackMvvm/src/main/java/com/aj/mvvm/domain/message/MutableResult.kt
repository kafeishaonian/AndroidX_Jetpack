package com.aj.mvvm.domain.message

class MutableResult<T> : Result<T> {

    constructor(value: T) : super(value)

    constructor() : super()

    /**
     * 勿在 Activity/Fragment 等页面处使用该 API，
     * 请在 SharedViewModel 等唯一可信源处使用该 API，
     *
     * @param value
     */
    public override fun setValue(value: T) {
        super.setValue(value)
    }

    /**
     * 勿在 Activity/Fragment 等页面处使用该 API，
     * 请在 SharedViewModel 等唯一可信源处使用该 API，
     *
     * @param value
     */
    public override fun postValue(value: T) {
        super.postValue(value)
    }


    inner class Builder<T> {
        private var isAllowNullValue = false

        fun setAllowNullValue(allowNullValue: Boolean): Builder<T> {
            this.isAllowNullValue = allowNullValue
            return this
        }

        fun create(): MutableResult<T> {
            val liveData: MutableResult<T> = MutableResult()
            liveData.isAllowNullValue = this.isAllowNullValue
            return liveData
        }
    }
}