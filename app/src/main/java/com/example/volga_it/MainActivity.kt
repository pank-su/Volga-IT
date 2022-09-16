package com.example.volga_it

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import java.io.InputStreamReader
import java.lang.Exception

class MainActivity : AppCompatActivity() {

    var startingActivity = false
    lateinit var data: JSONArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val inputStreamReader = InputStreamReader(baseContext.openFileInput("data.json"))
        data = JSONArray(inputStreamReader.readText())
        val editText = findViewById<EditText>(R.id.editTextText)
        val linearLayout: LinearLayout = findViewById(R.id.linearLayout4)
        val recyclerView: RecyclerView = findViewById(R.id.StockView)
        recyclerView.apply {
            layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = StocksAdapter(data, this@MainActivity)
        }
        linearLayout.visibility = View.GONE
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun afterTextChanged(p0: Editable?) {
                var new_data = JSONArray()
                println(editText.text.toString())
                if (editText.text.isNotBlank()) {
                    for (i in 0 until data.length()) {
                        val obj = data.getJSONObject(i)
                        val name = obj.getString("description")
                        if (editText.text.toString().lowercase() in name.lowercase()) {
                            new_data.put(obj)
                        }
                    }
                } else {
                    new_data = data
                }
                try {
                    findViewById<RecyclerView>(R.id.StockView).removeAllViews()

                    (recyclerView.adapter as StocksAdapter).data = new_data
                    (recyclerView.adapter as StocksAdapter).notifyDataSetChanged()
                } catch (e: Exception) {
                    println(e.printStackTrace())
                }

            }


        })
        findViewById<View>(R.id.floatingActionButton).setOnClickListener {
            if (linearLayout.visibility == View.GONE)
                linearLayout.visibility = View.VISIBLE
            else {
                editText.setText("")
                linearLayout.visibility = View.GONE
            }
        }


    }

    fun OnError() {
        if (!startingActivity) {
            findViewById<RecyclerView>(R.id.StockView).removeAllViews()
            startActivity(Intent(this, ErrorActivity::class.java))
            finish()
            startingActivity = true
        }

    }
}