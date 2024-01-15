package com.example.bluechat.data

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

//Broadcast receiver is required to scan for new devices
class FoundDeviceReceiver(
    private val onDeviceFound:(BluetoothDevice)->Unit
):BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when(intent?.action){
             BluetoothDevice.ACTION_FOUND->{
                 val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                     intent.getParcelableExtra(
                         BluetoothDevice.EXTRA_DEVICE,
                         BluetoothDevice::class.java
                     )
                 }
                 else{
                     intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE )
                 }
                 device?.let {
                     onDeviceFound
                 }
             }
        }
    }
}