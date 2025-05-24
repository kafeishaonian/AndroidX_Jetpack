package com.aj.mvvm.demo.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ShareViewModel : ViewModel() {

    val itemLiveData: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    fun clickItem(infoStr: String) {
        itemLiveData.value = infoStr
    }

}