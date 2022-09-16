package com.example.volga_it

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
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

class StocksAdapter(
    var data: JSONArray,
    private val activity: MainActivity,
    val webSocketWorker: WebSocketWorker,
    val webSocket: WebSocket,
    val isLiked: Boolean = false
) :
    RecyclerView.Adapter<StocksAdapter.StockViewHolder>() {

    private val retrofit = Retrofit.Builder().baseUrl("https://finnhub.io/api/v1/").build()


    class StockViewHolder(itemView: View, val activity: MainActivity) :
        RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.NameTextView)
        val symbol: TextView = itemView.findViewById(R.id.SymbolTextView)
        val _price: TextView = itemView.findViewById(R.id.PriceTextView)
        val _change: TextView = itemView.findViewById(R.id.DiffTextView)
        val pair = Pair<ImageView, ImageView>(
            itemView.findViewById(R.id.up),
            itemView.findViewById(R.id.down)
        )
        val liveIndicator: ImageView = itemView.findViewById(R.id.liveIndicator)
        var animated: AnimatedVectorDrawableCompat? = null
        var isLiked: ImageButton = itemView.findViewById(R.id.imageButton)
        var isLikedBool: Boolean = false
        val step: EditText = itemView.findViewById(R.id.editTextNumberDecimal)
        lateinit var current: JSONObject
        val cardView: CardView = itemView.findViewById(R.id.card)

        // Свойство которое задаёт цену
        var Price: Double
            get() = _price.text.toString().slice(0 until _change.text.toString().length - 1).toDouble()
            @SuppressLint("SetTextI18n")
            set(value) {
                val normalValue = Math.round(value * 1000) / 1000.0
                activity.runOnUiThread {
                    if (normalValue == 0.0)
                        _price.text = "Can't load"
                    else {
                        _price.text = "$value $"
                    }
                }
            }

        // Свойстов, которое обрабатывает изменения изменения и делает красивые стрелки и текст
        var Change: Double
            get() = _change.text.toString().slice(0 until _change.text.length - 1).toDouble()
            @SuppressLint("SetTextI18n")
            set(value) {
                val normalValue = Math.round(value * 1000) / 1000.0
                activity.runOnUiThread {
                    if (normalValue == 0.0) {
                        pair.first.visibility = View.GONE
                        pair.second.visibility = View.GONE
                        _change.visibility = View.GONE
                        return@runOnUiThread
                    }

                    if (normalValue > 0.0) {
                        pair.second.visibility = View.GONE
                        pair.first.visibility = View.VISIBLE
                        _change.setTextColor(
                            ContextCompat.getColor(
                                _change.context,
                                R.color.secondaryDark
                            )
                        )
                    } else if (normalValue < 0.0) {
                        pair.first.visibility = View.GONE
                        pair.second.visibility = View.VISIBLE
                        _change.setTextColor(
                            ContextCompat.getColor(
                                _change.context,
                                R.color.primaryDark
                            )
                        )
                    }
                    _change.visibility = View.VISIBLE
                    _change.text = "$normalValue $"
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
                if (_price.text == "...") {
                    taskToGetPrice = CoroutineScope(Dispatchers.Main).launch {
                        while (_price.text == "...") {
                            try {
                                val priceResponse = retrofit.create(ApiInterface::class.java)
                                    .getStockPrice(symbol.text.toString())
                                if (priceResponse.code() == 429) {
                                    delay(700)
                                    continue
                                }
                                if (priceResponse.code() == 403) {
                                    _price.setText("Can't load")
                                    return@launch
                                }
                                println(priceResponse.code())
                                if (priceResponse.code() != 200)
                                    throw Exception()
                                val priceJsonObject = JSONObject(priceResponse.body()!!.string())
                                // println(priceJsonObject.toString())
                                if (!priceJsonObject.isNull("c")) {
                                    Price = priceJsonObject.getDouble("c")
                                }
                                if (!priceJsonObject.isNull("d")) {
                                    Change = priceJsonObject.getDouble("d")
                                }
                            } catch (e: CancellationException) {
                                this.cancel()
                            } catch (e: Exception) {
                                println(e.printStackTrace())
                                activity.OnError()
                                this.cancel()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        return StockViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.res_item, parent, false), activity
        )
    }



    override fun onViewRecycled(holder: StockViewHolder) {
        /* Удаление неотображающих элементов из webSocket
        И сохранение цены с изменением, для того чтобы потом не прогружать
         */

        webSocketWorker.UnSubscribe(webSocket, holder)
        try {
            if (holder._price.text != "..." && holder._price.text != "Can't load")
                data.getJSONObject(holder.adapterPosition).put("price", holder._price.text)
            if (holder._change.text.toString() != "" && holder._change.text.toString() != "Измен.")
                data.getJSONObject(holder.adapterPosition).put("change", holder.Change)
            holder.timer.cancel()
            holder.taskToGetPrice?.cancel()
        } catch (e: Exception) {

        }
        super.onViewRecycled(holder)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {

        holder.apply {
            current = data.getJSONObject(position)
            /*
            По хорошему можно запрашивать имя, но мы тогда просто убиваем наш токен
            Так как максимум 30 запросов в минуту. Конечно можно попробовать имена записать
            в отдельный файл, но тогда при смене имени мы не будем знать что имя обновилось,
            а также у нас всего 26 000 акций
            */
            step.visibility = View.GONE
            if (this@StocksAdapter.isLiked) {
                step.visibility = View.VISIBLE
                step.setText(current.getDouble("step").toString())
                step.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {
                    }

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {
                    }

                    override fun afterTextChanged(s: Editable?) {
                        for (i in 0 until activity.likedData.length()) {
                            if (activity.likedData.getJSONObject(i)
                                    .getString("displaySymbol") == current.getString("displaySymbol")
                            ) {
                                try {
                                    activity.likedData.getJSONObject(i)
                                        .put("step", step.text.toString().toDouble())
                                    activity.openFileOutput("likes.json", Context.MODE_PRIVATE)
                                        .use {
                                            it.write(activity.likedData.toString().toByteArray())
                                        }
                                } catch (e: Exception) {

                                }

                            }
                        }
                    }
                })
            }
            isLiked.setImageResource(R.drawable.ic_baseline_star_border_24)
            if (activity.likedData.length() > 0)
                for (i in 0 until activity.likedData.length()) {
                    if (current.getString("displaySymbol") == activity.likedData.getJSONObject(i)
                            .getString("displaySymbol")
                    ) {
                        isLiked.setImageResource(R.drawable.ic_baseline_star_24)
                        isLikedBool = true
                        break
                    }
                }
            isLiked.setOnClickListener { _ ->

                if (isLikedBool) {
                    activity.unLike(current)
                    isLiked.setImageResource(R.drawable.ic_baseline_star_border_24)
                    isLikedBool = false
                } else {
                    if (activity.likedData.length() == 5) {
                        Toast.makeText(
                            activity,
                            "Извините, но в этой версии приложения, максимальное количество понравившихся акций - 5",
                            Toast.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }
                    activity.like(current)
                    isLiked.setImageResource(R.drawable.ic_baseline_star_24)
                    isLikedBool = true
                }

            }
            symbol.text = current.getString("displaySymbol")
            this.retrofit = this@StocksAdapter.retrofit

            name.text = current.getString("description").lowercase()
                // .toTitle
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

            animated = AnimatedVectorDrawableCompat.create(_change.context, R.drawable.live_anim)
            animated?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
                override fun onAnimationEnd(drawable: Drawable?) {
                    liveIndicator.post { animated?.start() }
                }
            })
            liveIndicator.setImageDrawable(animated)

            pair.first.visibility = View.GONE
            pair.second.visibility = View.GONE
            _change.visibility = View.GONE
            liveIndicator.visibility = View.GONE
            if (current.has("price")) {
                _price.text = current.getString("price")
                Change = if (current.has("change"))
                    current.getDouble("change")
                else 0.0
            } else {
                _price.text = "..."
                timer.start()
            }
            webSocketWorker.Subscribe(webSocket, this)
            cardView.setOnClickListener {
                    _ ->
                activity.startActivity(Intent(activity, ChartScreen::class.java).apply {
                    this.putExtra("name", name.text.toString())
                    this.putExtra("symbol", symbol.text.toString())
                })
            }
        }
    }

    override fun getItemCount(): Int = data.length()

}