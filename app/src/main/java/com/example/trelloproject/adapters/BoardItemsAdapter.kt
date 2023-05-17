package com.example.trelloproject.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.trelloproject.R
import com.example.trelloproject.models.Board

open class BoardItemsAdapter(private val context:Context, private var list: ArrayList<Board>):
    RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    private var onClickListener:OnClickListener?=null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return MyViewHolder(LayoutInflater.from(context)
            .inflate(R.layout.item_board
                ,parent,false))
    }

    override fun getItemCount(): Int {
        return list.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model=list[position]
        if(holder is MyViewHolder){
            Glide
                .with(context)
                .load(model.image)
                .centerCrop()
                .placeholder(R.drawable.ic_board_place_holder)
                .into(holder.ivBoardImage)
            holder.tvName.text=model.name
            holder.tvCreatedBy.text="Created By: ${model.createdBy}"
            holder.itemView.setOnClickListener {
                if(onClickListener!=null){
                    onClickListener!!.onClick(position,model)
                }
            }
        }
    }
    interface OnClickListener{
        fun onClick(position: Int,model:Board)
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

    private class MyViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val ivBoardImage: ImageView = view.findViewById(R.id.iv_board_image)
        val tvName: TextView = view.findViewById(R.id.tv_name)
        val tvCreatedBy: TextView = view.findViewById(R.id.tv_created_by)
    }
}