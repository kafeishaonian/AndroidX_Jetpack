package com.aj.mvvm.demo.livedata

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.map

class LiveDataFragment: Fragment() {

    companion object{
        fun newInstance() = LiveDataFragment()
    }

    private var randomStr by mutableStateOf("")

    private val changeObserver = Observer<String> { value ->
        value?.let {
            randomStr = value
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ComposeView(requireContext()).apply {
            setContent {
                Text(text = randomStr)
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val liveData = LiveDataInstance.INSTANCE.map {
            "Transform: $it"
        }
        liveData.observe(viewLifecycleOwner, changeObserver)
    }









}