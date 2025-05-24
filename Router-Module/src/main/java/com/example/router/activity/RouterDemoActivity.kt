package com.example.router.activity

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.router.AppAsm
import com.example.router.IFeed

import com.example.router.R

class RouterDemoActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_router)

        val feed = AppAsm.getRouter(IFeed::class.java).getFeed()
        Log.e("LogLogLog", "------------------> $feed")
    }

}