package com.example.bluechat.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluechat.domain.BluetoothDevice
import com.example.bluechat.domain.BluetoothDeviceDomain

@Composable
fun DeviceScreen(
    onStartScan:()->Unit,
    onStopScan:()->Unit,
    onDeviceClick:(BluetoothDevice)->Unit,
    onStartServer:()->Unit,
    state: BluetoothUiState,
    modifier: Modifier=Modifier
){
    Column(
        modifier = modifier.fillMaxSize()
    ) {

        BluetoothDeviceList(
            pairedDevices =state.pairedDevices ,
            scannedDevices =state.scannedDevices ,
            onClick =onDeviceClick,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        Row(
            modifier=Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = onStartScan) {
                Text(text = "Start Scan")
            }
            Button(onClick = onStopScan) {
                Text(text = "Stop Scan")
            }
            Button(onClick = onStartServer) {
                Text(text = "Start Server")
            }
        }
    }
}

@Composable
fun BluetoothDeviceList(
    pairedDevices:List<BluetoothDeviceDomain>,
    scannedDevices:List<BluetoothDeviceDomain>,
    onClick:(BluetoothDeviceDomain)->Unit,
    modifier: Modifier=Modifier
){
    LazyColumn(
        modifier=modifier
    ){
        //this is for paired devices
        // for adding constant items use the 'item' function
        item {
            Text(text = "Paired Devices", fontWeight = FontWeight.Bold,
                fontSize = 24.sp,modifier=Modifier.padding(16.dp))
        }
        items(pairedDevices){
            Text(text = it.name ?: "no name found",
                modifier= Modifier
                    .fillMaxWidth()
                    .clickable { onClick(it) }
                    .padding(16.dp)
            )
        }

        //this is for scanned devices
        item {
            Text(text = "Scanned Devices", fontWeight = FontWeight.Bold,
                fontSize = 24.sp,modifier=Modifier.padding(16.dp))
        }
        items(scannedDevices){
            Text(text = it.name ?: "no name found",
                modifier= Modifier
                    .fillMaxWidth()
                    .clickable { onClick(it) }
                    .padding(16.dp)
            )
        }
    }

}
















