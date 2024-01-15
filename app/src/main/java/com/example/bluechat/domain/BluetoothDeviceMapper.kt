package com.example.bluechat.domain

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice

@SuppressLint("MissingPermission")
fun BluetoothDevice.toBluetoothDeviceDomain():BluetoothDeviceDomain{
    return BluetoothDeviceDomain(
        name=name,
        address = address
    )
}