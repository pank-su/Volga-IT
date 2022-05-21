package com.example.volga_it

import android.annotation.SuppressLint
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

class WebSocketWorker(private val StocksAdapter: StocksAdapter) : WebSocketListener() {
    private val _holdersAndSymbols: MutableList<Pair<StocksAdapter.StockViewHolder, String>> = mutableListOf()
    var HoldersAndSymbols = _holdersAndSymbols

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Update(webSocket)
    }

    fun Update(webSocket: WebSocket){
        for (pair in HoldersAndSymbols)
            if (pair !in _holdersAndSymbols){
                webSocket.send("{\"type\":\"subscribe\",\"symbol\":\"${pair.second}\"}")
                _holdersAndSymbols.add(pair)
            }
        // TODO: Исправить удаление,
        _holdersAndSymbols.removeAll { pair -> pair.first.isRecyclable }
        HoldersAndSymbols = _holdersAndSymbols
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        println(text)
        Update(webSocket)
        val jsonObject = JSONObject(text).getJSONArray("data").getJSONObject(1)
        _holdersAndSymbols.find { pair -> pair.second == jsonObject.getString("s")}?.first?.Price?.text = jsonObject.getString("p")

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        println(reason)
        // по выходным биржа не работает, поэтому переходим в оффлайн режим,
        // где данные будут загружаться из кэша
        if (reason == "{\"type\":\"ping\"}") {
            StocksAdapter.offlineMode = true
            StocksAdapter.notifyDataSetChanged()
            // TODO: StocksAdapter должен научиться работать в офлайн режиме
        }
        super.onClosing(webSocket, code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        println(response?.code())
        super.onFailure(webSocket, t, response)
    }
}