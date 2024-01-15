package com.example.bluechat.data

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.example.bluechat.domain.BluetoothController
import com.example.bluechat.domain.BluetoothDeviceDomain
import com.example.bluechat.domain.BluetoothMessage
import com.example.bluechat.domain.ConnectionResult
import com.example.bluechat.domain.toBluetoothDeviceDomain
import com.example.bluechat.domain.toByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID


@SuppressLint("MissingPermission")
class AndroidBluetoothController(
    private val context: Context
):BluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }

    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private var bluetoothDataTransferService:BluetoothDataTransferService?=null

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _pairedDevices.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevices.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: SharedFlow<String>
        get() = _errors.asSharedFlow()

    //initializing the broadcaster
    private val foundDeviceReceiver = FoundDeviceReceiver{device->
        _scannedDevices.update { devices->
            val newDevice = device.toBluetoothDeviceDomain()
            if (newDevice in devices) devices else devices+newDevice
        }
    }

    private val bluetoothStateReceiver = BluetoothStateReceiver{isConnected,bluetoothDevice ->
        if (bluetoothAdapter?.bondedDevices?.contains(bluetoothDevice) == true){
            _isConnected.update {
                isConnected
            }
        }
        else{
            CoroutineScope(Dispatchers.IO).launch{
                _errors.emit("Cannot connect to a non-paired device")
            }
        }
    }


    init {
        updatePairedDevices()
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
        )
    }

    override fun startDiscovery() {
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN))return

        //we need to register the broadcast receiver
        context.registerReceiver(
            foundDeviceReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )

        updatePairedDevices()
        bluetoothAdapter?.startDiscovery()
    }

    override fun stopDiscovery() {
        // this stops the scanning process
        if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN))return
        bluetoothAdapter?.cancelDiscovery()
    }

    //we need 2 sockets for the client and the server
    private var currentServerSocket:BluetoothServerSocket?=null
    private var currentClientSocket:BluetoothSocket?= null

    override fun startBluetoothServer(): Flow<ConnectionResult> {
        return flow<ConnectionResult> {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT))
                throw SecurityException("BLUETOOTH_CONNECT permission denied")

            //this creates a socket with given uuid
           currentServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                "service_chat",
                UUID.fromString(SERVICE_UUID)
            )

            var shouldLoop = true
            while (shouldLoop){
                currentClientSocket=try {
                    currentServerSocket?.accept()
                }
                catch (e:IOException){
                    shouldLoop=false
                    null
                }
                emit(ConnectionResult.ConnectionEstablished)
                currentClientSocket?.let {
                    currentServerSocket?.close()
                    val service = BluetoothDataTransferService(it)
                    bluetoothDataTransferService = service

                    emitAll(
                        service.listenForIncomingMessages()
                            .map {bluetoothMessage->
                                ConnectionResult.TransferSucceeded(bluetoothMessage)
                            }
                    )
                }
            }
        }
            .onCompletion {
                closeConnection()
            }
            .flowOn(Dispatchers.IO)
    }

    override fun connectToDevice(deviceDomain: BluetoothDeviceDomain): Flow<ConnectionResult> {
        return flow {
            if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT))
                throw SecurityException("BLUETOOTH_CONNECT permission denied")

            //establishing a connection
            currentClientSocket =bluetoothAdapter
                ?.getRemoteDevice(deviceDomain.address)
                ?.createInsecureRfcommSocketToServiceRecord(
                    UUID.fromString(SERVICE_UUID)
                )
            //after establishing a connection we can stop discovering new devices
            stopDiscovery()

            /**connect and emit a [ConnectionResult] object for accessing*/
            currentClientSocket?.let {
                try {
                    it.connect()
                    emit(ConnectionResult.ConnectionEstablished)
                    BluetoothDataTransferService(it).also {service->
                        bluetoothDataTransferService=service
                        emitAll(
                            service.listenForIncomingMessages()
                                .map { msg->
                                ConnectionResult.TransferSucceeded(msg)
                            }
                        )
                    }
                }
                catch (e:IOException){
                    it.close()
                    currentClientSocket = null
                    emit(ConnectionResult.Error("Connection was interrupted"))
                }
            }
        }
            .onCompletion {
                closeConnection()
            }
            .flowOn(Dispatchers.IO)
    }

    override suspend fun trySendMessage(message: String): BluetoothMessage? {
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
            || bluetoothDataTransferService==null)return null

        val bluetoothMessage = BluetoothMessage(
            message = message,
            senderName = bluetoothAdapter?.name?:"Unknown device",
            isFromLocalUser = true
        )
        bluetoothDataTransferService?.sendMessage(bluetoothMessage.toByteArray())

        return bluetoothMessage
    }

    override fun closeConnection() {
        currentClientSocket?.close()
        currentServerSocket?.close()
        currentServerSocket=null
        currentClientSocket=null
    }

    override fun release() {
        //by unregistering we don't receive the results whe we leave the screen
       context.unregisterReceiver(foundDeviceReceiver)
        context.unregisterReceiver(bluetoothStateReceiver)
        closeConnection()
    }

    private fun updatePairedDevices(){
        if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT))return
        bluetoothAdapter
            ?.bondedDevices
            ?.map {
                it.toBluetoothDeviceDomain()
            }
            ?.also {devices->
                _pairedDevices.update { devices }
            }
    }

    private fun hasPermission(permission: String):Boolean{
        return context.checkSelfPermission(permission)==PackageManager.PERMISSION_GRANTED
    }
    companion object{
        const val SERVICE_UUID = "2f441ce1-6ee8-40da-81ef-239985b6f61c"
    }
}