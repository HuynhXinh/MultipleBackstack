package com.example.multiplebackstack.feature.card

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.multiplebackstack.R

class CardsAdapter(
    private val items: Array<String>,
    val onItemSelectedListener: (String) -> Unit
) : RecyclerView.Adapter<CardsAdapter.ViewHolder>() {

    class ViewHolder(val item: View) : RecyclerView.ViewHolder(item)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_card, parent, false)

        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.item.findViewById<TextView>(R.id.item_card_tv).text = items[position]

        holder.item.setOnClickListener {
            onItemSelectedListener.invoke(items[position])
        }
    }

    override fun getItemCount() = items.size
}