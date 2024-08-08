package com.jasonernst.packetdumper.serverdumper

import com.jasonernst.packetdumper.EtherType
import java.nio.ByteBuffer

class PcapNgTcpServerPacketDumper(
    val listenPort: Int = DEFAULT_PORT,
) : AbstractServerPacketDumper() {
    companion object {
        const val DEFAULT_PORT = 19000
    }

    override fun start() {
        TODO("Not yet implemented")
    }

    override fun stop() {
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
