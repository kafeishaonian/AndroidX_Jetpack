package com.aj.mvvm.domain.message

import com.aj.mvvm.ui.callback.ProtectedPeekLiveData

open class Result<T> : ProtectedPeekLiveData<T> {

    constructor(value: T): super(value)

    constructor(): super()
}