package com.example.volga_it

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
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
        val name: TextView = itemView.findViewById(R.id.NameTextView)
        val symbol: TextView = itemView.findViewById(R.id.SymbolTextView)
        val price: TextView = itemView.findViewById(R.id.PriceTextView)
        val change: TextView = itemView.findViewById(R.id.DiffTextView)
        val pair = Pair<ImageView, ImageView>(itemView.findViewById(R.id.up), itemView.findViewById(R.id.down))
        val liveIndicator: ImageView = itemView.findViewById(R.id.liveIndicator)
        var animated: AnimatedVectorDrawableCompat? = null


        // Свойстов, которое обрабатывает изменения изменения и делает красивые стрелки и текст
        var ChangeDouble: Double
            get() = change.text.toString().slice(0 until change.text.length - 1).toDouble()
            @SuppressLint("SetTextI18n")
            set(value) {
                val normalValue = Math.round(value * 10000) / 10000.0
                activity.runOnUiThread {
                    if (normalValue == 0.0) {
                        pair.first.visibility = View.GONE
                        pair.second.visibility = View.GONE
                        change.visibility = View.GONE
                        return@runOnUiThread
                    }

                    if (normalValue > 0.0){
                        pair.second.visibility = View.GONE
                        pair.first.visibility = View.VISIBLE
                        change.setTextColor(ContextCompat.getColor(change.context, R.color.secondaryDark))
                    }
                    else if (normalValue < 0.0){
                        pair.first.visibility = View.GONE
                        pair.second.visibility = View.VISIBLE
                        change.setTextColor(ContextCompat.getColor(change.context, R.color.primaryDark))
                    }
                    change.visibility = View.VISIBLE
                    change.text = "$normalValue $"
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
                if (price.text == "...") {
                    taskToGetPrice = CoroutineScope(Dispatchers.Main).launch {
                        while (price.text == "...") {
                            val priceResponse = retrofit.create(ApiInterface::class.java)
                                .getStockPrice(symbol.text.toString())
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
                                    price.text = "0 $"
                                else price.text = "$currentPrice $"
                            }
                            if (!priceJsonObject.isNull("d")) {
                                ChangeDouble = priceJsonObject.getDouble("d")
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
        /* Удаление неотображающих элементов из webSocket
        И сохранение цены с изменением, для того чтобы потом не прогружать
         */
        webSocketWorker.UnSubscribe(webSocket, holder)
        if (holder.price.text != "...")
            data.getJSONObject(holder.adapterPosition).put("price", holder.price.text)
        if (holder.change.text.toString() != "" && holder.change.text.toString() != "Измен.")
            data.getJSONObject(holder.adapterPosition).put("change", holder.ChangeDouble)
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
            symbol.text = current.getString("displaySymbol")
            this.retrofit = this@StocksAdapter.retrofit

            name.text = current.getString("description").lowercase()
                // .toTitle
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            animated = AnimatedVectorDrawableCompat.create(change.context, R.drawable.live_anim)
            animated?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    liveIndicator.post { animated?.start() }
                }
            })
            liveIndicator.setImageDrawable(animated)

            pair.first.visibility = View.GONE
            pair.second.visibility = View.GONE
            change.visibility = View.GONE
            liveIndicator.visibility = View.GONE
            if (current.has("price")) {
                price.text = current.getString("price")
                if (current.has("change"))
                    ChangeDouble = current.getDouble("change")
                else change.text = ""
            }
            else {
                price.text = "..."
                timer.start()
            }
            webSocketWorker.Subscribe(webSocket, this)

        }
    }

    override fun getItemCount(): Int = data.length()

}