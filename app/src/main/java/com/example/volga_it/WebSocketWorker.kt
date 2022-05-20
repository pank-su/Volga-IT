package com.example.volga_it

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

class WebSocketWorker(private val query: String, private val stockViewHolder: StocksAdapter.StockViewHolder) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        webSocket.send(query)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        println(text)
        // webSocket.send(message)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        println(reason)
        super.onClosing(webSocket, code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        println(response!!.code())
        super.onFailure(webSocket, t, response)
    }
}