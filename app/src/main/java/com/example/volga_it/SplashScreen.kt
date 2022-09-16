package com.example.volga_it

import android.content.Context
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
import java.io.FileNotFoundException
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
                    var likes = JSONArray()
                    try {
                        likes = JSONArray(
                            baseContext.openFileInput("likes.json").bufferedReader().readText()
                        )
                    } catch (e: FileNotFoundException) {
                        baseContext.openFileOutput("likes.json", Context.MODE_PRIVATE).use {
                            it.write("[]".toByteArray())
                        }
                    }
                    val response = retrofit.create(ApiInterface::class.java).getSymbols("AS")
                    if (response.code() != 200) {
                        throw Exception("Ошибка подключения")
                    }
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