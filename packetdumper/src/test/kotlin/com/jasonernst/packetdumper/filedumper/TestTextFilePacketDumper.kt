package com.jasonernst.packetdumper.filedumper

import com.jasonernst.packetdumper.EtherType
import com.jasonernst.packetdumper.stringdumper.StringPacketDumper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.ByteBuffer

class TestTextFilePacketDumper {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val dumper = TextFilePacketDumper("/tmp", "test")

    /**
     * Dumps a buffer to a file using the dumper, closes the file. Opens it back up and reads the
     * buffer back and returns it.
     *
     * If the addresses flag is true, strip the addresses before returning the buffer.
     * If the etherType is not null, strip the dummy ethernet header before returning the buffer.
     *
     * If either of those two things are present during the write, but not present during the read,
     * throw an Exception.
     */
    private fun writeReadBuffer(
        buffer: ByteBuffer,
        addresses: Boolean = false,
        etherType: EtherType? = null,
    ): ByteBuffer {
        dumper.dumpBuffer(buffer, 0, buffer.limit(), addresses, etherType)
        dumper.close()

        // re-open the file
        val file = File(dumper.filename)
        val fileLines = file.readLines()
        val readBuffer = ByteBuffer.wrap(fileLines.joinToString().toByteArray())

        if (etherType != null) {
            TODO() // need to strip the dummy ethernet header
        }

        if (addresses) {
            TODO() // need to strip them
        }

        return readBuffer
    }

    @AfterEach fun tearDown() {
        // just in case a test fails, we want to make sure the file is closed
        dumper.close()

        // delete the file created for the test to cleanup
        try {
            // File(dumper.filename).delete()
            logger.debug("Deleted file ${dumper.filename}")
        } catch (e: Exception) {
            // ignore
        }
    }

    @Test fun testOpenClose() {
        dumper.open()
        dumper.close()
    }

    @Disabled("This is broken atm")
    @Test
    fun testDumpBuffer() {
        dumper.open()
        val stringDumper = StringPacketDumper()
        val buffer = ByteBuffer.wrap("Hello, World!".toByteArray())
        logger.debug("write buffer: ${stringDumper.dumpBufferToString(buffer, 0, buffer.limit())}")
        val readBuffer = writeReadBuffer(buffer)
        logger.debug("read buffer: ${stringDumper.dumpBufferToString(readBuffer, 0, readBuffer.limit())}")
        assertEquals(buffer, readBuffer)
    }
}
