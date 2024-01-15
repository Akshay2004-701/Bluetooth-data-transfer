package com.example.bluechat.domain

sealed interface ConnectionResult {
    data object ConnectionEstablished:ConnectionResult
    data class TransferSucceeded(val message: BluetoothMessage):ConnectionResult
    data class Error(val message:String):ConnectionResult
}