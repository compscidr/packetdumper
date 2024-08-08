package com.jasonernst.packetdumper.filedumper

import com.jasonernst.packetdumper.EtherType
import com.jasonernst.packetdumper.stringdumper.StringPacketDumper
import org.slf4j.LoggerFactory
import java.io.BufferedWriter
import java.io.File
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean

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
 */
class TextFilePacketDumper(
    private val path: String,
    private val name: String,
) : AbstractFilePacketDumper() {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val stringDumper = StringPacketDumper()

    private val isOpen = AtomicBoolean(false)
    override lateinit var filename: String
    private lateinit var file: File
    private lateinit var bufferedWriter: BufferedWriter
    private var loggedError = false

    override fun open() {
        if (isOpen.get()) {
            logger.error("Trying to open a file that is already open")
            return
        }
        filename = "$path/${name}_${LocalDateTime.now()}.dump"
        file = File(filename)
        logger.debug("TextFilePacketDumper opened file $filename")
        isOpen.set(true)
    }

    override fun close() {
        if (!isOpen.get()) {
            logger.error("Trying to close a file that is already closed")
            return
        }
        isOpen.set(false)
    }

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
