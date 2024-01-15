package com.example.bluechat.domain

data class BluetoothMessage(
    val message:String,
    val senderName:String,
    val isFromLocalUser:Boolean
)
