package com.jasonernst.packetdumper.filedumper

import com.jasonernst.packetdumper.EtherType
import com.jasonernst.packetdumper.EthernetHeader
import com.jasonernst.packetdumper.stringdumper.StringPacketDumper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
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
        val stringDumper = StringPacketDumper()
        logger.debug("write buffer: ${stringDumper.dumpBufferToString(buffer, 0, buffer.limit())}")
        dumper.dumpBuffer(buffer, 0, buffer.limit(), addresses, etherType)
        dumper.close()

        // re-open the file
        val file = File(dumper.filename)
        var text = file.readText()
        logger.debug("raw text: $text")

        if (addresses) {
            val lines = text.split("\n")
            text = ""
            for (line in lines) {
                text += line.drop(10) + " "
            }
            text = text.trimEnd() // remove the trailing space
            logger.debug("No address text: $text")
        }

        if (etherType != null) {
            // note: we multiple by 3, because each byte is represented by two hex characters and a
            // space character
            // EthernetHeader.ETHERNET_HEADER_LENGTH.toInt() * 3
            text = text.drop(42)
            //text = text.trimEnd() // remove the trailing space
            logger.debug("No ether type text: $text")
        }

        // remove any remaining newlines
        text = text.replace("\n", " ")

        // each space separated value is a hex value, so we need to turn it back into bytes
        val readBuffer = ByteBuffer.wrap(text.split(" ").map { it.toInt(16).toByte() }.toByteArray())

        logger.debug("read buffer: ${stringDumper.dumpBufferToString(readBuffer, 0, readBuffer.limit())}")
        return readBuffer
    }

    @AfterEach fun tearDown() {
        // just in case a test fails, we want to make sure the file is closed
        dumper.close()

        // delete the file created for the test to cleanup
        try {
            File(dumper.filename).delete()
            logger.debug("Deleted file ${dumper.filename}")
        } catch (e: Exception) {
            // ignore
        }
    }

    @Test fun testOpenClose() {
        dumper.open()
        dumper.close()
    }

    /**
     * Test writing and reading from a file without using the dumper, just as a sanity check.
     */
    @Test fun testFileStringWriteRead() {
        dumper.open()
        val file = File(dumper.filename)
        file.writeText("Hello, World!")
        file.readText().also {
            assertEquals("Hello, World!", it)
        }
        file.delete()
    }

    @Test
    fun testDumpBuffer() {
        dumper.open()
        val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08))
        val readBuffer = writeReadBuffer(buffer)
        assertEquals(buffer, readBuffer)
    }

    @Test
    fun testDumpBufferWithAddresses() {
        dumper.open()
        val buffer =
            ByteBuffer.wrap(
                byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10),
            )
        val readBuffer = writeReadBuffer(buffer, addresses = true)
        assertEquals(buffer, readBuffer)
    }

    @Test
    fun testDumpBufferWithEtherType() {
        dumper.open()
        val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08))
        val readBuffer = writeReadBuffer(buffer, etherType = EtherType.IPv4)
        assertEquals(buffer, readBuffer)
    }

    @Test
    fun testDumpBufferWithAddressesAndEtherType() {
        dumper.open()
        val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08))
        val readBuffer = writeReadBuffer(buffer, addresses = true, etherType = EtherType.IPv4)
        assertEquals(buffer, readBuffer)
    }
}
