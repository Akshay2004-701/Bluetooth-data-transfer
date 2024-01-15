package com.example.bluechat

import android.app.Application
import com.example.bluechat.data.Container
import com.example.bluechat.data.DefaultContainer

class BluetoothApplication:Application() {
    lateinit var container: Container
    override fun onCreate() {
        super.onCreate()
        container = DefaultContainer(this)
    }
}