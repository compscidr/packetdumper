package com.jasonernst.packetdumper

import java.nio.ByteBuffer

/**
 * Represents an Ethernet or Wi-Fi MAC address.
 */
class MACAddress {
    var bytes: ByteArray

    init {
        bytes = ByteArray(6)
    }

    constructor(bytes: ByteArray) {
        if (bytes.size != 6) {
            throw IllegalArgumentException("MAC address must be 6 bytes")
        }
        this.bytes = bytes
    }

    constructor(address: String) {
        this.bytes = address.split(":").map { it.toInt(16).toByte() }.toByteArray()
    }

    override fun toString(): String = bytes.joinToString(":") { it.toString(16).padStart(2, '0') }

    companion object {
        fun fromStream(stream: ByteBuffer): MACAddress {
            val bytes = ByteArray(6)
            stream.get(bytes)
            return MACAddress(bytes)
        }

        val DUMMY_MAC_SOURCE = MACAddress("14:c0:3e:55:0b:35")
        val DUMMY_MAC_DEST = MACAddress("74:d0:2b:29:a5:18")
    }
}
