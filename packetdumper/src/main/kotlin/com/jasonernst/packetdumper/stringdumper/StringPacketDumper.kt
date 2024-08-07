package com.jasonernst.packetdumper.stringdumper

import com.jasonernst.packetdumper.AbstractPacketDumper
import com.jasonernst.packetdumper.EtherType
import com.jasonernst.packetdumper.EthernetHeader
import com.jasonernst.packetdumper.EthernetHeader.Companion
import com.jasonernst.packetdumper.EthernetHeader.Companion.prependDummyHeader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer

/**
 * A packet dumper that will dump packets to a string. This is useful for live debugging. It can
 * either log to a slf4f logger if one is provided, or it can write to stdout. By default it does
 * neither, however, it can still be used manually by calling `dumpBufferToString`.
 * @param packetLogger The slf4j logger to dump the buffers to.
 * @param writeToStdOut If true, will write to stdout (if the logger is null)
 */
class StringPacketDumper(private val packetLogger: Logger? = null, private val writeToStdOut: Boolean = false) : AbstractPacketDumper() {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * In this case calling dump buffer will:
     * a) write to an slf4j logger if the constructor arg is not null
     * b) write to stdout if no logger has been above, and if the writeToStdout constructor arg is true
     */
    override fun dumpBuffer(
        buffer: ByteBuffer,
        offset: Int,
        length: Int,
        addresses: Boolean,
        etherType: EtherType?,
    ) {
        val hexString = dumpBufferToString(buffer, offset, length, addresses, etherType)
        if (packetLogger != null) {
            packetLogger.info(hexString)
        } else if (writeToStdOut) {
            println(hexString)
        }
    }

    /**
     * This function will dump the buffer to a string and return it.
     *
     * Given the byte buffer, converts the bytes to a Hexadecimal string. Puts the buffer back to
     * the position it started in before returning.
     *
     * - Spaces each byte
     * - Newline after every 16 bytes
     *
     * Optionally:
     * - Prints the offset addresses at the start of the line
     * - Addresses start at 0x00000000 and increment by 16, regardless of the offset. The buffer
     *   position + the offset = the starting address of 0.
     *
     * @param buffer the buffer to convert
     * @param offset the offset to start at
     * @param length the number of bytes to convert
     *   (if offset + length > buffer.limit(), will print up to buffer.limit())
     * @param addresses if true, will print the offset addresses at the start of the line
     *   (compatible with a wireshark hex dump import)
     * @param etherType the ethertype of the buffer if known, or 0xFFFF if unknown. null if it is
     *   not desired to prepend with an ethernet header
     */
    fun dumpBufferToString(
        buffer: ByteBuffer,
        offset: Int = 0,
        length: Int = 0,
        addresses: Boolean = false,
        etherType: EtherType? = null,
    ): String {
        val startingPosition = buffer.position()
        // optionally prepend the ethernet dummy header
        val conversionBuffer = if (etherType != null) {
            prependDummyHeader(buffer, offset, length, etherType)
        } else {
            val totalLength = minOf(length, buffer.limit() - offset)
            if (totalLength < length) {
                logger.warn("Trying to dump more bytes than are in the buffer. Dumping up to buffer limit.")
            }
            val newBuffer = ByteBuffer.allocate(totalLength)
            newBuffer.put(buffer.array(), offset, totalLength)
            newBuffer.rewind()
            newBuffer
        }
        val output = StringBuilder()
        var i = 0
        while (i < conversionBuffer.limit()) {
            if (addresses && (i % 16 == offset % 16)) {
                output.append(String.format("%08X  ", i - (offset % 16) - (16 * (offset / 16))))
            }
            output.append(String.format("%02X", buffer.get()))
            i++
            if (i > offset && i % 16 == offset % 16) {
                output.append("\n")
            } else if (i < (length + offset)) {
                output.append(" ")
            }
        }
        buffer.position(startingPosition)
        return output.toString()
    }
}
