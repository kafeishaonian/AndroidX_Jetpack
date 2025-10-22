package com.aj.demo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.module_fundamental.IFeedLog
import com.example.module_fundamental.cement2.CementAdapter
import com.example.module_fundamental.cement2.CementModel
import com.example.module_fundamental.cement2.eventhook.OnClickEventHook
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

            startActivity(Intent(this, OOMActivity::class.java))
        }



        val recycler = findViewById<RecyclerView>(R.id.recycler)
        val adapter = CementAdapter()
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
        adapter.addEventHook(object: OnClickEventHook<TextItemView.ViewHolder>(TextItemView.ViewHolder::class.java){
            override fun onClick(
                view: View,
                viewHolder: TextItemView.ViewHolder,
                position: Int,
                rawModel: CementModel<*>
            ) {
                Log.e("LogLogLog", "--------> position:= $position")
            }

            override fun onBind(viewHolder: TextItemView.ViewHolder): View? {
                return viewHolder.textView
            }

        })

        val list = mutableListOf<CementModel<*>>()
        (0..20).forEach {
            list.add(TextItemView("你好啊啊 $it"))
        }
        adapter.addModels(list)
    }
}