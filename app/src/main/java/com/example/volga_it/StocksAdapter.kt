package com.example.volga_it

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import java.util.*

class StocksAdapter(private val data: JSONArray, private val retrofit: Retrofit, var offlineMode: Boolean = false) :
    RecyclerView.Adapter<StocksAdapter.StockViewHolder>() {
    val request = Request.Builder().url("wss://ws.finnhub.io?token=c900veqad3icdhuein80").build()
    val client = OkHttpClient()
    var webSocketWorker: WebSocketWorker = WebSocketWorker(this)
    lateinit var webSocket: WebSocket



    class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val Name = itemView.findViewById<TextView>(R.id.NameTextView)
        val Symbol = itemView.findViewById<TextView>(R.id.SymbolTextView)
        val Price = itemView.findViewById<TextView>(R.id.PriceTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        return StockViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.res_item, parent, false))
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
            webSocketWorker.HoldersAndSymbols.add(Pair(holder, current.getString("displaySymbol")))
            if (position == 0) {
                webSocket = client.newWebSocket(request, webSocketWorker)
            }else{
                webSocketWorker.Update(webSocket)
            }
            Name.text = current.getString("description").lowercase().capitalize(Locale.getDefault())
        }
        // client.newWebSocket(request, WebSocketWorker("{\"type\":\"subscribe\",\"symbol\":\"${current.getString("displaySymbol")}\"}", holder))

    }

    override fun getItemCount(): Int = data.length()

}