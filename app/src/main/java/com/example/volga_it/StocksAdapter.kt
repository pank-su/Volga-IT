package com.example.volga_it

import android.annotation.SuppressLint
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Retrofit
import java.util.*

class StocksAdapter(private val data: JSONArray, private val activity: MainActivity) :
    RecyclerView.Adapter<StocksAdapter.StockViewHolder>() {
    private val request =
        Request.Builder().url("wss://ws.finnhub.io?token=c900veqad3icdhuein80").build()
    private val retrofit = Retrofit.Builder().baseUrl("https://finnhub.io/api/v1/").build()
    private val client = OkHttpClient()
    private val webSocketWorker: WebSocketWorker = WebSocketWorker()
    private val webSocket: WebSocket = client.newWebSocket(request, webSocketWorker)


    class StockViewHolder(itemView: View, val activity: MainActivity) : RecyclerView.ViewHolder(itemView) {
        val Name = itemView.findViewById<TextView>(R.id.NameTextView)
        val Symbol = itemView.findViewById<TextView>(R.id.SymbolTextView)
        val Price = itemView.findViewById<TextView>(R.id.PriceTextView)
        val Change = itemView.findViewById<TextView>(R.id.DiffTextView)
        val pair = Pair<ImageView, ImageView>(itemView.findViewById(R.id.up), itemView.findViewById(R.id.down))
        val liveIndicator = itemView.findViewById<ImageView>(R.id.liveIndicator)


        // Свойстов, которое обрабатывает изменения изменения и делает красивые стрелки и текст
        var ChangeDouble: Double
            get() = Change.text.toString().toDouble()
            set(value) {
                activity.runOnUiThread {
                    if (value == 0.0) {
                        pair.first.visibility = View.GONE
                        pair.second.visibility = View.GONE
                        Change.visibility = View.GONE
                        return@runOnUiThread
                    }

                    if (value > 0.0){
                        pair.second.visibility = View.GONE
                        pair.first.visibility = View.VISIBLE
                        Change.setTextColor(ContextCompat.getColor(Change.context, R.color.secondaryDark))
                    }
                    else if (value < 0.0){
                        pair.first.visibility = View.GONE
                        pair.second.visibility = View.VISIBLE
                        Change.setTextColor(ContextCompat.getColor(Change.context, R.color.primaryDark))
                    }
                    Change.visibility = View.VISIBLE
                    Change.text = "${Math.round(value * 100000) / 100000.0f} $"
                }
            }
        lateinit var retrofit: Retrofit
        var taskToGetPrice: Job? = null

        /* Если цена не взялась из веб-сокета(таймаут 1 секунда), например акция не продаётся в данный момент,
        то обновляем цену через обычное api.
        */
        val timer = object : CountDownTimer(1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {

            }

            @SuppressLint("SetTextI18n")
            override fun onFinish() {
                if (Price.text == "...") {
                    taskToGetPrice = CoroutineScope(Dispatchers.Main).launch {
                        while (Price.text == "...") {
                            val priceResponse = retrofit.create(ApiInterface::class.java)
                                .getStockPrice(Symbol.text.toString())
                            if (priceResponse.code() == 429) {
                                delay(700)
                                continue
                            }
                            if (priceResponse.code() != 200)
                                continue
                            val priceJsonObject = JSONObject(priceResponse.body()!!.string())
                            // println(priceJsonObject.toString())
                            if (!priceJsonObject.isNull("c")) {
                                val currentPrice = priceJsonObject.getDouble("c")
                                if (currentPrice == 0.0)
                                    Price.text = "0 $"
                                else Price.text = "$currentPrice $"
                            }
                            if (!priceJsonObject.isNull("d")) {
                                val diff = priceJsonObject.getDouble("d")
                                ChangeDouble = diff
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        return StockViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.res_item, parent, false), activity)
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
            в отдельный файл, но тогда при смене имени мы не будем знать что имя обновилось,
            а также у нас всего 26 000 акций
            */
            Symbol.text = current.getString("displaySymbol")
            this.retrofit = this@StocksAdapter.retrofit

            Name.text = current.getString("description").lowercase()
                // .toTitle
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            if (current.has("price"))
                Price.text = current.getString("price")
            else {
                Price.text = "..."
                timer.start()
            }
            Change.text = ""
            pair.first.visibility = View.GONE
            pair.second.visibility = View.GONE
            Change.visibility = View.GONE
            liveIndicator.visibility = View.GONE
            webSocketWorker.Subscribe(webSocket, this)
            timer.start()
        }
    }

    override fun getItemCount(): Int = data.length()

}