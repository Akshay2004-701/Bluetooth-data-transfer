package com.example.bluechat.presentation

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothApp(
     viewModel: BluetoothViewModel = viewModel(factory=BluetoothViewModel.Factory),
){
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = {
                Text(
                    text = "Blue Chat",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            })
        }
    ) {


        val ctx = LocalContext.current
        val state by viewModel.state.collectAsState()

        LaunchedEffect(key1 = state.errorMessage){
            state.errorMessage?.let {message->
                Toast.makeText(
                    ctx,
                    message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        LaunchedEffect(key1 = state.isConnected ){
            if (state.isConnected){
                Toast.makeText(
                    ctx,
                    "Your device is connected",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        when{

            state.isConnecting-> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Text(text = "Connecting...")
                }
            }

            state.isConnected->{
                MessageScreen(
                    state = state,
                    onDisconnect = viewModel::disconnectFromDevice,
                    onSendMsg = viewModel::sendMessage
                )
            }

            else-> {
                DeviceScreen(
                    onStartScan = viewModel::startScan,
                    onStopScan = viewModel::stopScan,
                    state = state,
                    onDeviceClick = viewModel::connectToDevice,
                    onStartServer = viewModel::waitForIncomingConnections,
                    modifier = Modifier.padding(it)
                )
            }
        }

        }
    }
