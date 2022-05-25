package com.example.volga_it

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import java.util.*

class StocksAdapter(private val data: JSONArray) :
    RecyclerView.Adapter<StocksAdapter.StockViewHolder>() {
    val request = Request.Builder().url("wss://ws.finnhub.io?token=c900veqad3icdhuein80").build()
    val retrofit = Retrofit.Builder().baseUrl("https://finnhub.io/api/v1/").build()
    val client = OkHttpClient()
    val webSocketWorker: WebSocketWorker = WebSocketWorker()
    val webSocket: WebSocket = client.newWebSocket(request, webSocketWorker)



    class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val Name = itemView.findViewById<TextView>(R.id.NameTextView)
        val Symbol = itemView.findViewById<TextView>(R.id.SymbolTextView)
        val Price = itemView.findViewById<TextView>(R.id.PriceTextView)
        val Change = itemView.findViewById<TextView>(R.id.DiffTextView)
        lateinit var retrofit: Retrofit
        var taskToGetPrice: Job? = null

        /* Если цена не взялась из веб-сокета(таймаут 1 секунда), например акция не продаётся в данный момент,
        то обновляем цену через обычное api.
        */
        val timer = object: CountDownTimer(1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {

            }

            @SuppressLint("SetTextI18n")
            override fun onFinish() {
                if (Price.text == "..."){
                    taskToGetPrice = CoroutineScope(Dispatchers.Main).launch{
                        while (Price.text == "..."){
                            val priceResponse = retrofit.create(ApiInterface::class.java).getStockPrice(Symbol.text.toString())
                            if (priceResponse.code() == 429){
                                delay(700)
                                continue
                            }
                            val priceJsonObject = JSONObject(priceResponse.body()!!.string())
                            // println(priceJsonObject.toString())
                            if (!priceJsonObject.isNull("c")){
                                val currentPrice = priceJsonObject.getDouble("c")
                                if (currentPrice == 0.0)
                                    Price.text = "0 $"
                                else Price.text = "$currentPrice $"
                            }
                            if (!priceJsonObject.isNull("d")) {
                                val diff = priceJsonObject.getDouble("d")
                                if (diff == 0.0)
                                    Change.text = ""
                                else Change.text = "$diff $"
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        return StockViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.res_item, parent, false))
    }

    override fun onViewRecycled(holder: StockViewHolder) {
        // Удаление неотображающих элементов из webSocket
        webSocketWorker.UnSubscribe(webSocket, holder)
        data.getJSONObject(holder.adapterPosition).put("price", holder.Price.text)
        holder.timer.cancel()
        holder.taskToGetPrice?.cancel()
        super.onViewRecycled(holder)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val current = data.getJSONObject(position)
        holder.apply {
            /*
            По хорошему можно запрашивать имя, но мы тогда просто убиваем наш токен
            Так как максимум 30 запросов в минуту. Конечно можно попробовать имена записать
            в отдельный файл, но тогда при смене имени мы не будем знать что имя обновилось
            */
            Symbol.text = current.getString("displaySymbol")
            this.retrofit = this@StocksAdapter.retrofit
            Name.text = current.getString("description").lowercase().capitalize(Locale.getDefault())
            if (current.has("price"))
                Price.text = current.getString("price")
            else{
                Price.text = "..."
                timer.start()
            }
            Change.text = ""
            webSocketWorker.Subscribe(webSocket, this)
            timer.start()
        }
    }

    override fun getItemCount(): Int = data.length()

}