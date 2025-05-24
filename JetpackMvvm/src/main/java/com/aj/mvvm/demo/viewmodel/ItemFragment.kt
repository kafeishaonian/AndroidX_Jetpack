package com.aj.mvvm.demo.viewmodel

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

class ItemFragment: Fragment() {

    private val mShareMode by lazy {
        ViewModelProvider(requireActivity()).get(ShareViewModel::class.java)
    }

}