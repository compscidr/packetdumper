package com.jasonernst.packetdumper

import com.jasonernst.packetdumper.ethernet.EtherType
import java.nio.ByteBuffer

abstract class AbstractPacketDumper {
    /**
     * Dumps a ByteBuffer starting at position offset, and going until position length. If length
     * + offset is greater than buffer.remaining(), it will dump until the end of the buffer.
     *
     * If addresses is true, it will dump the address offsets with offset being position zero. The
     * addresses will be dumped at the left hand side of the dump, followed by whitespace, and then
     * the hexdump of the buffer. This only really applies to text dumpers or string dumpers and not
     * pcap dumpers.
     *
     * If etherType is set, a dummy ethernet header will be prepended to to the start of the dump.
     * This is particularly useful for pcap format with wireshark since it allows for wireshark to
     * just recognize the traffic immediately.
     *
     * Dumping the buffer should not change the:
     * - position, limit, etc of the buffer.
     */
    abstract fun dumpBuffer(
        buffer: ByteBuffer,
        offset: Int = 0,
        length: Int = buffer.remaining(),
        addresses: Boolean = false,
        etherType: EtherType? = null,
    )
}
