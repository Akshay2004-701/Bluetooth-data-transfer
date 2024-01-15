package com.example.bluechat.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val isConnected:StateFlow<Boolean>
    val scannedDevices:StateFlow<List<BluetoothDevice>>
    val pairedDevices:StateFlow<List<BluetoothDevice>>
    val errors:SharedFlow<String>

    fun startDiscovery()
    fun stopDiscovery()

    fun startBluetoothServer():Flow<ConnectionResult>
    fun connectToDevice(deviceDomain: BluetoothDeviceDomain):Flow<ConnectionResult>
    suspend fun trySendMessage(message: String):BluetoothMessage?

    fun closeConnection()
    fun release()
}