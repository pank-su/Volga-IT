package com.example.volga_it

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import retrofit2.Retrofit
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.lang.Exception

class SplashScreen : AppCompatActivity() {
    val retrofit = Retrofit.Builder().baseUrl("https://finnhub.io/api/v1/").build()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        CoroutineScope(Dispatchers.Main).launch {
            for (i in 0..3){
                try {
                    val response = retrofit.create(ApiInterface::class.java).getSymbols()
                    if (response.code() != 200) {
                        throw Exception("Ошибка подключения")
                    }
                    // println(response.body()!!.string())
                    // JSONArray(response.body()!!.string())
                    val streamWriter = OutputStreamWriter(applicationContext.openFileOutput("data.json", MODE_PRIVATE))
                    streamWriter.write(response.body()!!.string())
                    streamWriter.close()
                    this@SplashScreen.startActivity(Intent(this@SplashScreen, MainActivity::class.java))
                    this@SplashScreen.finish()
                    return@launch
                } catch (e: Exception) {
                    println(e.printStackTrace())
                }
            }
            this@SplashScreen.startActivity(Intent(this@SplashScreen, ErrorActivity::class.java))
            finish()
        }
    }
}