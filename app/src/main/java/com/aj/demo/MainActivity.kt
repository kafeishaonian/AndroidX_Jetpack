package com.aj.demo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aj.nativelib.IP2pSource
import com.example.router.AppAsm
//import com.aj.mvvm.demo.compiler.CompilerActivity
//import com.aj.nativelib.PublicIPFetcher
//import com.aj.nativelib.ThreadDemo
import com.example.router.activity.RouterDemoActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
//        val node = ThreadDemo.initP2P(7777, 7776)
//        Thread(Runnable{
//            Log.e("LogLogLog", "------> node:= $node     IP:= ${PublicIPFetcher.getPublicIP()}")
//            ThreadDemo.requestConnectToPeer("39.91.87.155", 7777)
//        }).start()

//        ThreadDemo.requestConnectToPeer("192.168.88.215", 105)
//        ThreadDemo.setP2PCallback { ip->
//            ThreadDemo.sendData(ip, data = "hello from main")
//        }
        startActivity(Intent(this, RouterDemoActivity::class.java))

        val p2p = AppAsm.getRouter(IP2pSource::class.java).getPublicIP()
        Log.e("LogLogLog", "-----> p2p:= $p2p")
    }


    override fun onDestroy() {
        super.onDestroy()
        Log.e("LogLogLog", "-----> onDestroy")
//        ThreadDemo.destroyP2P()
    }
}