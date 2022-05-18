package com.example.volga_it

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class StocksAdapter(private val data: JSONArray) :
    RecyclerView.Adapter<StocksAdapter.StockViewHolder>() {
    val request = Request.Builder().url("wss://ws.finnhub.io?token=c900veqad3icdhuein80").build()
    val client = OkHttpClient()

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
            holder.Name.text = current.getString("description")
            holder.Symbol.text = current.getString("displaySymbol")
        }
    }

    override fun getItemCount(): Int = data.length()

}