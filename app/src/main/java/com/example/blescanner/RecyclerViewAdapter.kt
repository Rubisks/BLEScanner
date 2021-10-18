package com.example.blescanner

import android.bluetooth.le.ScanResult
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.scan_result.view.deviceNameTv
import kotlinx.android.synthetic.main.scan_result.view.deviceMacTv
import kotlinx.android.synthetic.main.scan_result.view.signalStrengthTv
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.scan_result.view.*

class ScanResultAdapter(private val deviceList: List<ScanResult>,
                        ): RecyclerView.Adapter<ScanResultAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.scan_result,
            parent, false)

        return ViewHolder(itemView)
    }
    override fun getItemCount() = deviceList.size

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = deviceList[position]

        holder.bind(currentItem)
    }



    //
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        fun bind(result: ScanResult){
            itemView.deviceNameTv.text = result.device.name ?: "unnamed"
            itemView.deviceMacTv.text = result.device.address
            itemView.signalStrengthTv.text = "${result.rssi} dbm"
        }

    }
}