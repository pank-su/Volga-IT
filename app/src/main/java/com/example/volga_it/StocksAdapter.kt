package com.example.volga_it

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import org.json.JSONArray
import java.util.*

class StocksAdapter(private val data: JSONArray) :
    RecyclerView.Adapter<StocksAdapter.StockViewHolder>() {
    val request = Request.Builder().url("wss://ws.finnhub.io?token=c900veqad3icdhuein80").build()
    val client = OkHttpClient()
    val webSocketWorker: WebSocketWorker = WebSocketWorker()
    val webSocket: WebSocket = client.newWebSocket(request, webSocketWorker)



    class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val Name = itemView.findViewById<TextView>(R.id.NameTextView)
        val Symbol = itemView.findViewById<TextView>(R.id.SymbolTextView)
        val Price = itemView.findViewById<TextView>(R.id.PriceTextView)
        val Change = itemView.findViewById<TextView>(R.id.DiffTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        return StockViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.res_item, parent, false))
    }

    override fun onViewRecycled(holder: StockViewHolder) {
        // TODO: Сделать удаление элементов, которые не отображаются
        // webSocketWorker.Opened.removeAll { pair -> pair.first == holder }
        // val saves = webSocketWorker.Opened
        //webSocketWorker = WebSocketWorker(this, webSocketWorker.Opened)
        //webSocket.close(1000, "Restart with new connection")
        //client.newWebSocket(request, webSocketWorker)
        webSocketWorker.UnSubscribe(webSocket, holder)
        super.onViewRecycled(holder)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val current = data.getJSONObject(position)
        holder.apply {
            Symbol.text = current.getString("displaySymbol")
            /*
            По хорошему можно запрашивать имя, но мы тогда просто убиваем наш токен
            Так как максимум 30 запросов в минуту. Конечно можно попробовать имена записать
            в отдельный файл, но тогда при смене имени мы не будем знать что имя обновилось
            */
            webSocketWorker.Subscribe(webSocket, this)
            Name.text = current.getString("description").lowercase().capitalize(Locale.getDefault())
        }
        // client.newWebSocket(request, WebSocketWorker("{\"type\":\"subscribe\",\"symbol\":\"${current.getString("displaySymbol")}\"}", holder))

    }

    override fun getItemCount(): Int = data.length()

}