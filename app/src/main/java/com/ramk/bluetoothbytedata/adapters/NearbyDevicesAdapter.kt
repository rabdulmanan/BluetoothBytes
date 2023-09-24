package com.ramk.bluetoothbytedata.adapters

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout.LayoutParams
import androidx.core.view.setMargins
import androidx.recyclerview.widget.RecyclerView
import com.ramk.bluetoothbytedata.databinding.NearbyDeviceBinding

class NearbyDevicesAdapter(private var nearbyDevices: List<BluetoothDevice>,
                    private val deviceSelectListener: NearbyDeviceClickListener?)
    :RecyclerView.Adapter<NearbyDevicesAdapter.NearbyDeviceViewHolder>(){

    fun setNearByDevices(devices: List<BluetoothDevice>){
        nearbyDevices = devices
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return nearbyDevices.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NearbyDeviceViewHolder {
        val mBinding = NearbyDeviceBinding.inflate(LayoutInflater.from(parent.context))

        return NearbyDeviceViewHolder(mBinding)
    }

    override fun onBindViewHolder(holder: NearbyDeviceViewHolder, position: Int) {
        val device = nearbyDevices[position]

        val layoutParam = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        layoutParam.setMargins(8)
        holder.nearbyDeviceBinding.root.layoutParams = layoutParam
        holder.nearbyDeviceBinding.neadyByDevice = device

        holder.nearbyDeviceBinding.root.setOnClickListener {
            deviceSelectListener?.onNearbyDeviceClicked(device)
        }

    }



    class NearbyDeviceViewHolder(val nearbyDeviceBinding: NearbyDeviceBinding)
        : RecyclerView.ViewHolder(nearbyDeviceBinding.root)


    interface NearbyDeviceClickListener{
        fun onNearbyDeviceClicked(device: BluetoothDevice)
    }
}