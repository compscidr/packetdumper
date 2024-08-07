package com.jasonernst.packetdumper

import java.nio.ByteBuffer

/**
 * A dummy implementation of the packet dumper that does nothing. Useful so that you can use a
 * real dumper in a debug implementation, and switch to the DummyPacketDumper for production.
 */
object DummyPacketDumper : AbstractPacketDumper() {
    override fun dumpBuffer(
        buffer: ByteBuffer,
        offset: Int,
        length: Int,
        addresses: Boolean,
        etherType: EtherType?,
    ) {
        // do nothing
    }
}
