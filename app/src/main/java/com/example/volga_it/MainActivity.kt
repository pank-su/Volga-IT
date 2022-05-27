package com.example.volga_it

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AlertDialog
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
    val retrofit = Retrofit.Builder().baseUrl("https://finnhub.io/api/v1/").build()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val inputStreamReader = InputStreamReader(baseContext.openFileInput("data.json"))
        val data = inputStreamReader.readText()
        findViewById<RecyclerView>(R.id.StockView).apply {
            layoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = StocksAdapter(JSONArray(data), this@MainActivity)
        }
    }
}