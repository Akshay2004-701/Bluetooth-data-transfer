package com.example.bluechat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.bluechat.BluetoothApplication
import com.example.bluechat.domain.BluetoothController
import com.example.bluechat.domain.BluetoothDevice
import com.example.bluechat.domain.BluetoothDeviceDomain
import com.example.bluechat.domain.BluetoothMessage
import com.example.bluechat.domain.ConnectionResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BluetoothViewModel(
    private val bluetoothController: BluetoothController
):ViewModel() {

    private var _state = MutableStateFlow(BluetoothUiState())

    //we declare a job and initiate in a function which can be cancelled later
    private var deviceConnectionJob:Job?=null

    val state = combine(
        bluetoothController.scannedDevices,
        bluetoothController.pairedDevices,
        _state
    ){scannedDevices,pairedDevices,state->
        state.copy(
            scannedDevices=scannedDevices,
            pairedDevices=pairedDevices,
            messages = if (state.isConnected) state.messages else emptyList()
        )
    }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            _state.value
        )

    init {
        //update the state when the booleans change
        bluetoothController.isConnected.onEach {isConnected->
            _state.update {
                it.copy(isConnected=isConnected)
            }
        }.launchIn(viewModelScope)

        bluetoothController.errors.onEach {error->
            _state.update {
                it.copy(errorMessage = error)
            }
        }.launchIn(viewModelScope)

    }

    fun connectToDevice(device: BluetoothDeviceDomain){
        _state.update { it.copy(isConnecting = true) }
        deviceConnectionJob = bluetoothController.connectToDevice(device).listen()
    }

    fun disconnectFromDevice(){
        deviceConnectionJob?.cancel()
        bluetoothController.closeConnection()
        _state.update { it.copy(isConnecting = false, isConnected = false) }
    }

    fun waitForIncomingConnections(){
        _state.update { it.copy(isConnecting = true) }
        deviceConnectionJob = bluetoothController.startBluetoothServer().listen()
    }

    fun sendMessage(message:String){
        viewModelScope.launch {
            val msg = bluetoothController.trySendMessage(message)
            if (msg !=null){
                _state.update {
                    it.copy(
                        messages = it.messages + msg
                    ) }
            }
        }
    }

    fun startScan(){
        bluetoothController.startDiscovery()
    }

    fun stopScan(){
        bluetoothController.stopDiscovery()
    }

    private fun Flow<ConnectionResult>.listen():Job{
        return onEach { connectionResult->
            when(connectionResult){
                ConnectionResult.ConnectionEstablished->{
                    _state.update {
                        it.copy(isConnected = true,
                            isConnecting = false,
                            errorMessage = null)
                    }
                }

                is ConnectionResult.TransferSucceeded->{
                    _state.update { it.copy(
                        messages = it.messages + connectionResult.message
                    ) }
                }

                is ConnectionResult.Error->{
                    _state.update {
                        it.copy(
                            isConnected = false,
                            isConnecting = false,
                            errorMessage = connectionResult.message
                        )
                    }
                }
            }
        }
            .catch {throwable->
                bluetoothController.closeConnection()
                _state.update {
                    it.copy(
                        isConnected = false,
                        isConnecting = false,
                        errorMessage = throwable.message
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    companion object{
        val Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as BluetoothApplication)
                BluetoothViewModel(application.container.bluetoothController)
            }
        }
    }

    /**this overridden function is required to release all the resources when th view model leaves the backstack */
    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }

}
data class BluetoothUiState(
    val scannedDevices:List<BluetoothDeviceDomain> = emptyList(),
    val pairedDevices :List<BluetoothDeviceDomain> = emptyList(),
    val messages:List<BluetoothMessage> = emptyList(),
    val isConnected :Boolean = false,
    val isConnecting:Boolean = false,
    val errorMessage:String? = null
)