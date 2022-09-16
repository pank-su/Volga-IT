package com.example.volga_it

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isVisible
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import kotlin.math.absoluteValue
import kotlin.random.Random


class WebSocketWorker(val context: Context) : WebSocketListener() {
    // Список получяемых акций

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
        webSocket.send("{\"type\":\"subscribe\",\"symbol\":\"${ToOpenHolder.symbol.text}\"}")
        Opened.add(Pair(ToOpenHolder, ToOpenHolder.symbol.text.toString()))
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
        webSocket.send("{\"type\":\"unsubscribe\",\"symbol\":\"${ToCloseHolder.symbol.text}\"}")
        Opened.remove(Pair(ToCloseHolder, ToCloseHolder.symbol.text.toString()))
    }


    @SuppressLint("SetTextI18n")
    override fun onMessage(webSocket: WebSocket, text: String) {
        // println(text)
        val jsonObject = JSONObject(text)
        if (jsonObject.has("type") && jsonObject.getString("type") == "ping") {
            println("Возможно биржа закрыта")
            return
        }
        try {
            val data = JSONObject(text).getJSONArray("data").getJSONObject(0)
            val new_price = data.getDouble("p")
            Opened.find { pair -> pair.second == data.getString("s") }!!.first.apply {
                if (this._price.text != "..." && this._price.text != "Can't load")
                    Change = if (new_price > Price) new_price - Price else Price - new_price
                if (step.visibility == View.VISIBLE && current.getDouble("step") < Change.absoluteValue) {
                    var builder = NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.logo)
                        .setContentTitle("Ваша акция изменила значение")
                        .setContentText(
                            "Цена вашей акции: ${data.getDouble("p")}, Изменение: $Change"
                        )
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    with(NotificationManagerCompat.from(context)) {
                        notify(Random.nextInt(100000), builder.build())
                    }
                }
                this.Price = new_price

                activity.runOnUiThread {
                    if (!liveIndicator.isVisible) {
                        timer.cancel()
                        liveIndicator.visibility = View.VISIBLE
                        animated?.start()
                    }

                }
            }
        } catch (e: Exception) {
        }
        super.onMessage(webSocket, text)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        // println(reason)
        // по выходным биржа не работает, поэтому переходим в оффлайн режим,
        // где данные будут загружаться из кэша, но
        // из-за большого количества акций, я не могу собрать должный список акций
        super.onClosing(webSocket, code, reason)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        // println(response?.code())
        val ToOpen = Opened.toList()
        Opened.clear()
        Subscribe(webSocket, ToOpen)
        super.onFailure(webSocket, t, response)
    }
}