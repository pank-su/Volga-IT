package com.example.volga_it

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class WebSocketWorker() : WebSocketListener() {
    val Opened: MutableList<Pair<StocksAdapter.StockViewHolder, String>> = mutableListOf()

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Subscribe(webSocket)
    }

    fun Subscribe(
        webSocket: WebSocket,
        ToOpen: List<Pair<StocksAdapter.StockViewHolder, String>> = listOf(),
    ) {
        for (pair in ToOpen)
            if (pair !in Opened) {
                webSocket.send("{\"type\":\"subscribe\",\"symbol\":\"${pair.second}\"}")
                Opened.add(pair)
            }
    }

    fun Subscribe(webSocket: WebSocket, ToOpenHolder: StocksAdapter.StockViewHolder) {
        webSocket.send("{\"type\":\"subscribe\",\"symbol\":\"${ToOpenHolder.Symbol.text}\"}")
        Opened.add(Pair(ToOpenHolder, ToOpenHolder.Symbol.text.toString()))
    }

    fun UnSubscribe(
        webSocket: WebSocket,
        ToClose: List<Pair<StocksAdapter.StockViewHolder, String>> = listOf(),
    ) {
        for (pair in ToClose) {
            webSocket.send("{\"type\":\"unsubscribe\",\"symbol\":\"${pair.second}\"}")
            if (pair in Opened)
                Opened.remove(pair)
        }
    }

    fun UnSubscribe(webSocket: WebSocket, ToCloseHolder: StocksAdapter.StockViewHolder) {
        webSocket.send("{\"type\":\"unsubscribe\",\"symbol\":\"${ToCloseHolder.Symbol.text}\"}")
        Opened.remove(Pair(ToCloseHolder, ToCloseHolder.Symbol.text.toString()))
    }


    override fun onMessage(webSocket: WebSocket, text: String) {
        println(text)
        val jsonObject = JSONObject(text)
        if (jsonObject.has("type") && jsonObject.getString("type") == "ping") {
            Toast.makeText(Opened[0].first.Symbol.context,
                "Похоже, биржа закрыта",
                Toast.LENGTH_SHORT).show()
        }
        try {
            val data = JSONObject(text).getJSONArray("data").getJSONObject(0)
            Opened.find { pair -> pair.second == data.getString("s") }?.first?.Price?.text =
                data.getDouble("p").toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        println(reason)
        // по выходным биржа не работает, поэтому переходим в оффлайн режим,
        // где данные будут загружаться из кэша, но
        // из-за большого количества акций, я не могу собрать должный список акций
        // if (reason == "{\"type\":\"ping\"}") {
        // StocksAdapter.offlineMode = true
        // StocksAdapter.notifyDataSetChanged()
        // }
        super.onClosing(webSocket, code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        println(response?.code())
        super.onFailure(webSocket, t, response)
    }
}