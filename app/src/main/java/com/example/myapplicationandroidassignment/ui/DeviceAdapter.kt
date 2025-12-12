package com.example.myapplicationandroidassignment.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplicationandroidassignment.R
import com.example.myapplicationandroidassignment.data.Device


class DeviceAdapter(
    private val onDeviceClick: (Device) -> Unit
) : ListAdapter<Device, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return DeviceViewHolder(view, onDeviceClick)
    }
    
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class DeviceViewHolder(
        itemView: View,
        private val onDeviceClick: (Device) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val tvDeviceName: TextView = itemView.findViewById(R.id.tvDeviceName)
        private val tvIpAddress: TextView = itemView.findViewById(R.id.tvIpAddress)
        private val ivStatus: ImageView = itemView.findViewById(R.id.ivStatus)
        
        fun bind(device: Device) {
            tvDeviceName.text = device.deviceName
            tvIpAddress.text = device.ipAddress
            
            if (device.isOnline) {
                ivStatus.setColorFilter(Color.GREEN)
            } else {
                ivStatus.setColorFilter(Color.GRAY)
            }
            
            itemView.setOnClickListener {
                onDeviceClick(device)
            }
        }
    }
    
    class DeviceDiffCallback : DiffUtil.ItemCallback<Device>() {
        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.ipAddress == newItem.ipAddress
        }
        
        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem == newItem
        }
    }
}