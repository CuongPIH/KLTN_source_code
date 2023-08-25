package com.example.smart_control_swiftlet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CustomAdapter(private val mList: List<ItemsViewModel>) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    // create new views
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // inflates the card_view_design view
        // that is used to hold list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_view_design, parent, false)

        return ViewHolder(view)
    }

    // binds the list items to a view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val ItemsViewModel = mList[position]

        // sets the image to the imageview from our itemHolder class
        holder.temptv.text = "Nhiệt độ: " + ItemsViewModel.temp.toString() + " °C"
        holder.humtv.text = "Độ ẩm: " + ItemsViewModel.hum.toString() + " %"
        holder.lighttv.text = "Độ sáng: " + ItemsViewModel.light.toString() + " lux"
        holder.modetv.text = if (ItemsViewModel.mode == 1)  "Chế độ tự động" else "Chế độ thủ công"
        holder.timetv.text = "Thời gian: " + ItemsViewModel.time
        holder.fantv.text = "Quạt: " + if (ItemsViewModel.fan == 1)  " Bật" else " Tắt"
        holder.misttv.text = "Phun sương: " + if (ItemsViewModel.mist == 1)  " Bật" else " Tắt"
        holder.speaker.text = "Tiếng chim: " + if (ItemsViewModel.speaker == 1)  " Bật" else " Tắt"

    }

    // return the number of the items in the list
    override fun getItemCount(): Int {
        return mList.size
    }

    // Holds the views for adding it to image and text
    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val timetv: TextView = itemView.findViewById(R.id.timeTextview)
        val temptv: TextView = itemView.findViewById(R.id.tempTextview)
        val humtv: TextView = itemView.findViewById(R.id.humTextview)
        val lighttv: TextView = itemView.findViewById(R.id.lightTextview)
        val fantv: TextView = itemView.findViewById(R.id.fanTextview)
        val modetv: TextView = itemView.findViewById(R.id.modeTextview)
        val misttv: TextView = itemView.findViewById(R.id.mistTextview)
        val speaker: TextView = itemView.findViewById(R.id.speakerTextview)
    }
}