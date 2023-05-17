package com.example.trelloproject.adapters

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.trelloproject.R

class LabelColorListItemsAdapter(
    private val context: Context,
    private var list: ArrayList<String>,
    private val mSelectorColor: String
) : RecyclerView.Adapter<LabelColorListItemsAdapter.MyViewHolder>() {

    var onItemClickListener: OnItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(
            LayoutInflater.from(context).inflate(R.layout.item_label_color, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = list[position]
        holder.itemView.findViewById<View>(R.id.view_main).setBackgroundColor(Color.parseColor(item))
        if (item == mSelectorColor) {
            holder.itemView.findViewById<ImageView>(R.id.iv_selected_color).visibility = View.VISIBLE
        } else {
            holder.itemView.findViewById<ImageView>(R.id.iv_selected_color).visibility = View.GONE
        }
        holder.itemView.setOnClickListener {
            if (onItemClickListener != null) {
                onItemClickListener!!.onClick(position, item)
            }
        }
    }

    inner class MyViewHolder(view: View) : RecyclerView.ViewHolder(view)

    interface OnItemClickListener {
        fun onClick(position: Int, color: String)
    }
}
