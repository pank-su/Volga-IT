package com.example.volga_it

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton

class ErrorActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error)
        findViewById<ImageButton>(R.id.exitButton).setOnClickListener { _ -> finish() }
        findViewById<ImageButton>(R.id.restartButton).setOnClickListener { _ ->
            startActivity(Intent(this,
                SplashScreen::class.java))
        }
    }
}