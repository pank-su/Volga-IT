package com.example.volga_it

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.absoluteValue
import kotlin.random.Random

class NotificationService : Service() {
    lateinit var thread: MyFknThread

    override fun onCreate() {

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        thread = MyFknThread(this)
        thread.start()
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

}

class MyFknThread(val notificationService: NotificationService) : Thread() {
    override fun start() {
        super.start()
        val new_token = "cci70oiad3ie5k93d1m0"

        val client = OkHttpClient()
        val request =
            Request.Builder().url("wss://ws.finnhub.io?token=$new_token").build()
        val webSocketWorker: WebBackSocketWorker = WebBackSocketWorker(notificationService)
        val webSocket: WebSocket = client.newWebSocket(request, webSocketWorker)
        while (true) {
            val likes = JSONArray(
                notificationService.openFileInput("likes.json").bufferedReader().readText()
            )
            for (pair in webSocketWorker.Opened) {
                var isUnliked = true
                for (i in 0 until likes.length()) {
                    val new_pair = Pair(
                        likes.getJSONObject(i),
                        likes.getJSONObject(i).getString("displaySymbol")
                    )
                    if (new_pair == pair)
                        isUnliked = false
                    if (new_pair !in webSocketWorker.Opened) {
                        webSocketWorker.Subscribe(webSocket, pair)
                    }
                }
                if (isUnliked) {
                    webSocketWorker.UnSubscribe(webSocket, pair)
                }
            }
            sleep(60000)

        }
    }
}

class WebBackSocketWorker(val context: Context) : WebSocketListener() {
    // Список получяемых акций
    val Opened: MutableList<Pair<JSONObject, String>> = mutableListOf()

    fun Subscribe(
        webSocket: WebSocket,
        ToOpen: Pair<JSONObject, String>,
    ) {
        if (ToOpen !in Opened) {
            webSocket.send("{\"type\":\"subscribe\",\"symbol\":\"${ToOpen.second}\"}")
            Opened.add(ToOpen)
        }
    }

    fun UnSubscribe(webSocket: WebSocket, ToClose: Pair<JSONObject, String>) {
        webSocket.send("{\"type\":\"unsubscribe\",\"symbol\":\"${ToClose}\"}")
        Opened.remove(ToClose)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        println(text)
        val jsonObject = JSONObject(text).getJSONArray("data").getJSONObject(0)
        val stock = Opened.find { pair -> pair.second == jsonObject.getString("s") }!!
        if (jsonObject.getDouble("v").absoluteValue > stock.first.getDouble("step")) {
            var builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("Ваша акция изменила значение")
                .setContentText(
                    "Цена вашей акции: ${jsonObject.getDouble("p")}, Изменение: ${
                        jsonObject.getDouble(
                            "v"
                        )
                    }"
                )
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            with(NotificationManagerCompat.from(context)) {
                notify(Random.nextInt(100000), builder.build())
            }
        }
        super.onMessage(webSocket, text)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        // println(response?.code())
        val ToOpen = Opened.toList()
        Opened.clear()
        for (el in ToOpen)
            Subscribe(webSocket, el)
        super.onFailure(webSocket, t, response)
    }

}