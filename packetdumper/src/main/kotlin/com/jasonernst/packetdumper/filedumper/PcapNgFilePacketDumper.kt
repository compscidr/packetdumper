package com.jasonernst.packetdumper.filedumper

import com.jasonernst.packetdumper.EtherType
import java.nio.ByteBuffer

class PcapNgFilePacketDumper(
    override var filename: String,
) : AbstractFilePacketDumper() {
    override fun open() {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun dumpBuffer(
        buffer: ByteBuffer,
        offset: Int,
        length: Int,
        addresses: Boolean,
        etherType: EtherType?,
    ) {
        TODO("Not yet implemented")
    }
}
