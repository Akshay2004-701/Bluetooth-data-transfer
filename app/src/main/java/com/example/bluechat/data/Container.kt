package com.example.bluechat.data

import android.content.Context
import com.example.bluechat.domain.BluetoothController

interface Container {
    val bluetoothController:BluetoothController
}

class DefaultContainer( private val context: Context):Container{
    override val bluetoothController: BluetoothController by lazy {
        AndroidBluetoothController(context)
    }
}