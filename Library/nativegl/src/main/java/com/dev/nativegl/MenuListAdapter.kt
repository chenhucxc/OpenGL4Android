package com.dev.nativegl

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * description : clone from https://github.com/githubhaohao/NDK_OpenGLES_3_0
 *
 * @author     : hudongxin
 * @date       : 8/12/21
 */
class MenuListAdapter(private val context: Context, val data: List<String>) :
    RecyclerView.Adapter<MenuListAdapter.MyViewHolder>(), View.OnClickListener {

    private var mSelectIndex = 0
    private var mOnItemClickListener: OnItemClickListener? = null

    fun setSelectIndex(index: Int) {
        mSelectIndex = index
    }

    fun getSelectIndex(): Int {
        return mSelectIndex
    }

    fun addOnItemClickListener(onItemClickListener: OnItemClickListener) {
        mOnItemClickListener = onItemClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.sample_item_layout, parent, false)
        val viewHolder = MyViewHolder(view)
        view.setOnClickListener(this)
        return viewHolder
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.mTitle.text = data[position]
        if (position == mSelectIndex) {
            holder.mRadioButton.isChecked = true
            holder.mTitle.setTextColor(context.resources.getColor(R.color.colorAccent))
        } else {
            holder.mRadioButton.isChecked = false
            holder.mTitle.text = data[position]
            holder.mTitle.setTextColor(Color.GRAY)
        }
        holder.itemView.tag = position
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onClick(view: View?) {
        mOnItemClickListener?.onItemClick(view, view?.tag as Int)
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mRadioButton: RadioButton = itemView.findViewById(R.id.radio_btn)
        val mTitle: TextView = itemView.findViewById(R.id.item_title)
    }

    interface OnItemClickListener {
        fun onItemClick(view: View?, position: Int)
    }
}