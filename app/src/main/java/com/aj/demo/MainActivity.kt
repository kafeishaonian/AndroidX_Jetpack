package com.aj.demo

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.module_fundamental.IFeedLog
import com.example.router.log.ELog

class MainActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ELog.log(IFeedLog::class.java).showHomePage("123456789")

        val text = findViewById<TextView>(R.id.main_text)
        text.setOnClickListener {

            ELog.log(IFeedLog::class.java).clickProfile(map = mapOf(
                "id" to "123",
                "name" to "456"))
        }
    }
}