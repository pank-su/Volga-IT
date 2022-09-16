package com.example.volga_it

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import android.widget.TextView

class ChartScreen : AppCompatActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart_screen)
        findViewById<TextView>(R.id.textView5).text = intent.getStringExtra("name")
        val webView = findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("https://widget.finnhub.io/widgets/stocks/chart?symbol=${intent.getStringExtra("symbol")}&watermarkColor=%231db954&backgroundColor=%23222222&textColor=white")
    }
}