package com.jasonernst.packetdumper.pcapng

interface PcapNgBlock {
    fun size(): UInt

    fun toBytes(): ByteArray
}
