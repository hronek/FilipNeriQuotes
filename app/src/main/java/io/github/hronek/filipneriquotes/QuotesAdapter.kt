package io.github.hronek.filipneriquotes

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class QuotesAdapter(private var textSizeSp: Float) : RecyclerView.Adapter<QuotesAdapter.VH>() {
    private val items = mutableListOf<String>()

    fun submit(data: List<String>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    fun setTextSize(sizeSp: Float) {
        textSizeSp = sizeSp
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_quote, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val t = items[position]
        holder.tv.text = t
        holder.tv.textSize = textSizeSp
    }

    override fun getItemCount(): Int = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tv: TextView = v.findViewById(R.id.tvLine)
    }
}
