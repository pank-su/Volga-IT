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
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    val retrofit = Retrofit.Builder().baseUrl("https://finnhub.io/api/v1/").build()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Эта карутина должна быть на SplashScreen
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = retrofit.create(ApiInterface::class.java).getSymbols()
                if (response.code() != 200) {
                    throw Exception("Ошибка подключения")
                }
                // println(response.body()!!.string())
                val symbols = JSONArray(response.body()!!.string())

                findViewById<RecyclerView>(R.id.StockView).apply {
                    layoutManager =
                        LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                    adapter = StocksAdapter(symbols, retrofit)
                }

            } catch (e: Exception) {
                println(e.printStackTrace())
                AlertDialog.Builder(this@MainActivity)
                    .setNeutralButton("OK") { _, _ -> this@MainActivity.recreate() }
                    .setMessage("Ошибка подключения").create().show()
            }

        }

//        findViewById<Button>(R.id.button).setOnClickListener { _ ->
//            client.newWebSocket(request, WebSocketWorker("{\"type\":\"subscribe\",\"symbol\":\"AAPL\"}"))
//        }
    }
}