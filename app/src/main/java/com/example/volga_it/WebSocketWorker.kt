package com.example.volga_it

import android.annotation.SuppressLint
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class WebSocketWorker(
    private val StocksAdapter: StocksAdapter,
    val ToOpen: MutableList<Pair<StocksAdapter.StockViewHolder, String>> = mutableListOf(),
) : WebSocketListener() {
    val Opened: MutableList<Pair<StocksAdapter.StockViewHolder, String>> = mutableListOf()
    // val UnusedCount = 0

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Update(webSocket)
    }

    fun Update(webSocket: WebSocket) {
        for (pair in ToOpen)
            if (pair !in Opened) {
                webSocket.send("{\"type\":\"subscribe\",\"symbol\":\"${pair.second}\"}")
                Opened.add(pair)
            }
        ToOpen.clear()
        // Opened.count { pair -> pair.first.layoutPosition <  }

    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        println(text)
        Update(webSocket)
        try {
            val jsonObject = JSONObject(text).getJSONArray("data").getJSONObject(0)
            Opened.find { pair -> pair.second == jsonObject.getString("s") }?.first?.Price?.text =
                jsonObject.getString("p")
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        println(reason)
        // по выходным биржа не работает, поэтому переходим в оффлайн режим,
        // где данные будут загружаться из кэша
        // Из-за большого количества акций, я не могу собрать должный список акций
//        if (reason == "{\"type\":\"ping\"}") {
//            StocksAdapter.offlineMode = true
//            StocksAdapter.notifyDataSetChanged()
//        }
        super.onClosing(webSocket, code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        println(response?.code())
        super.onFailure(webSocket, t, response)
    }
}