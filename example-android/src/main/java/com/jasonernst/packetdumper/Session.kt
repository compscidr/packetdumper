package com.jasonernst.packetdumper

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

data class Session(
    val sourceAddress: String,
    val sourcePort: Int,
    val destinationAddress: String,
    val destinationPort: Int,
    val protocol: String,
    val timeStarted: Long
) {
    val outgoingPackets = mutableIntStateOf(0)
    val incomingPackets = mutableIntStateOf(0)
    val outgoingBytes = mutableIntStateOf(0)
    val incomingBytes = mutableIntStateOf(0)

    fun key() = getKey(sourceAddress, sourcePort, destinationAddress, destinationPort, protocol)

    companion object {
        fun getKey(
            sourceAddress: String,
            sourcePort: Int,
            destinationAddress: String,
            destinationPort: Int,
            protocol: String
        ): String {
            return "$sourceAddress:$sourcePort -> $destinationAddress:$destinationPort protocol: $protocol"
        }
    }
}