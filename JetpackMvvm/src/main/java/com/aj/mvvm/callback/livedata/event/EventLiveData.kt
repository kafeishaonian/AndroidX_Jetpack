package com.aj.mvvm.callback.livedata.event

import com.aj.mvvm.ui.callback.PeekLiveData

/**
 * 发送消息LiveData使用
 * 在Activity中observe 调用observeInActivity 在Fragment中使用调用 observeInFragment
 */
class EventLiveData<T> : PeekLiveData<T>()