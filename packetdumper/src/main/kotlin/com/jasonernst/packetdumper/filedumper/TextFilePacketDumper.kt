package com.jasonernst.packetdumper.filedumper

import com.jasonernst.packetdumper.ethernet.EtherType
import com.jasonernst.packetdumper.stringdumper.StringPacketDumper
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer

/**
 * Dumps packets to a text file in hexdump format.
 *
 * If addresses and EtherType is set, the output will be compatible with Wireshark Hexdump. It
 * can be imported with `File -> Import from Hexdump.`
 */
class TextFilePacketDumper(
    path: String,
    name: String,
) : AbstractFilePacketDumper(path, name, "txt") {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val stringDumper = StringPacketDumper()

    /**
     * Dumps buffers into hexdump text files that can be imported by wireshark. The format is as follows:
     *
     * offset(hex)  up to 16 bytes of data per line, separated by a space, ending in a newline
     * offset(hex)  up to 16 bytes of data per line, separated by a space, ending in a newline
     *
     * offset(hex)  up to 16 bytes of data per line, separated by a space, ending in a newline
     * offset(hex)  up to 16 bytes of data per line, separated by a space, ending in a newline
     *
     * Each block of offsets is separated by a blank newline, and represents a new packet / dump. When
     * the new dump starts, the offset can restart at 0.
     *
     */
    override fun dumpBuffer(
        buffer: ByteBuffer,
        offset: Int,
        length: Int,
        addresses: Boolean,
        etherType: EtherType?,
    ) {
        if (!isOpen.get()) {
            if (!loggedError) {
                logger.error("Trying to dump to a file that is not open")
                loggedError = true
            }
            return
        }
        val output = stringDumper.dumpBufferToString(buffer, offset, length, addresses, etherType)
        file.writeText(output)
    }
}
