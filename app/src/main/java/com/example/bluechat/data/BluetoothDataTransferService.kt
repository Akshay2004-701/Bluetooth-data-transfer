package com.example.bluechat.data

import android.bluetooth.BluetoothSocket
import com.example.bluechat.domain.BluetoothMessage
import com.example.bluechat.domain.ConnectionResult
import com.example.bluechat.domain.toBluetoothMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.IOError
import java.io.IOException

class BluetoothDataTransferService (
    private val socket:BluetoothSocket
){
    fun listenForIncomingMessages():Flow<BluetoothMessage>{
        return flow {
            if (!socket.isConnected)return@flow
            val buffer = ByteArray(1024)
            while (true){
                /** the info is stored in the buffer and size is returned to [byteCount]*/
                val byteCount = try {
                    socket.inputStream.read(buffer)
                }
                catch (e:IOException){
                  throw  IOException("Data transfer failed")
                }

                // decode the buffer until the byte count
                emit(
                    buffer.decodeToString(endIndex = byteCount).
                    toBluetoothMessage(false)
                    )
            }
        }.flowOn(Dispatchers.IO)
    }

    suspend fun sendMessage(bytes:ByteArray):Boolean{
        return withContext(Dispatchers.IO){
            try {
                socket.outputStream.write(bytes)
            }
            catch (e:IOException){
                e.printStackTrace()
                return@withContext false
            }

            true
        }
    }
}