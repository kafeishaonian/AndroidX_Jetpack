package com.aj.mvvm.demo.livedata

import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import com.aj.mvvm.ui.theme.BasicsCodeLabTheme

class LiveDataActivity: AppCompatActivity() {

    var randomStr by mutableStateOf("Activity中发送")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BasicsCodeLabTheme {
                Content(randomStr = randomStr)
            }
        }
    }

    fun updateValue() {
        sendData(produceData())
    }

    @Composable
    private fun Content(randomStr: String, modifier: Modifier = Modifier){
        Column(
            modifier = modifier.height(50.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(randomStr)
            Button(onClick = {
                updateValue()
            }) {
                Text("更新")
            }
            FragmentHost(LiveDataFragment.newInstance())
        }
    }

    private fun produceData(): String {
        val randomValue = (0..1000).random().toString()
        randomStr = "Activity中发送：$randomValue"
        return randomValue
    }

    private fun sendData(randomValue: String) {
        LiveDataInstance.INSTANCE.value = randomValue
    }

    @Composable
    private fun FragmentHost(fragment: Fragment) {
        val fragmentManager = supportFragmentManager
        var containerId by remember { mutableIntStateOf(View.generateViewId()) }

        AndroidView(
            factory = { ctx ->
                FragmentContainerView(ctx).apply {
                    id = containerId
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        DisposableEffect(Unit) {
            fragmentManager.commit {
                replace(containerId, fragment)
            }
            onDispose {
                fragmentManager.findFragmentById(containerId)?.let {
                    fragmentManager.beginTransaction().remove(it).commit()
                }
            }
        }
    }


    override fun onStop() {
        super.onStop()
        val data = produceData()
        sendData(data)
    }

}