package com.jasonernst.packetdumper.filedumper

import com.jasonernst.packetdumper.ethernet.EtherType
import com.jasonernst.packetdumper.ethernet.EthernetHeader
import com.jasonernst.packetdumper.pcapng.PcapNgBlock
import com.jasonernst.packetdumper.pcapng.PcapNgEnhancedPacketBlock
import com.jasonernst.packetdumper.pcapng.PcapNgInterfaceDescriptionBlock
import com.jasonernst.packetdumper.pcapng.PcapNgSectionHeaderBlockLive
import com.jasonernst.packetdumper.pcapng.PcapNgSimplePacketBlock
import com.jasonernst.packetdumper.stringdumper.StringPacketDumper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.nio.ByteBuffer

class TestPcapNgFilePacketDumper {
    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var dumper: PcapNgFilePacketDumper

    @AfterEach
    fun tearDown() {
        // just in case a test fails, we want to make sure the file is closed
        dumper.close()

        // delete the file created for the test to cleanup
        try {
            // File(dumper.filename).delete()
            // logger.debug("Deleted file ${dumper.filename}")
        } catch (e: Exception) {
            // ignore
        }
    }

    /**
     * Verify that the file has the correct headers, advances the readBuffer beyond these headers
     * and returns a list of the blocks.
     */
    private fun verifyHeaders(readBuffer: ByteBuffer): List<PcapNgBlock> {
        val pcapBlocks = mutableListOf<PcapNgBlock>()

        // we expect the file to start with a section header block
        pcapBlocks.add(PcapNgSectionHeaderBlockLive.fromStream(readBuffer))

        // we expect the file to have an interface description block
        pcapBlocks.add(PcapNgInterfaceDescriptionBlock.fromStream(readBuffer))

        return pcapBlocks
    }

    private fun readFile(): ByteBuffer {
        val readBuffer = ByteBuffer.wrap(BufferedInputStream(FileInputStream(dumper.filename)).readAllBytes())
        val stringPacketDumper = StringPacketDumper(logger)
        stringPacketDumper.dumpBuffer(readBuffer, 0, readBuffer.limit(), false, null)
        return readBuffer
    }

    /**
     * Test that the file is created and the correct blocks are written at the start.
     */
    @Test fun testOpenClose() {
        dumper = PcapNgFilePacketDumper("/tmp", "test")
        dumper.open()
        dumper.close()
        val readBuffer = readFile()
        verifyHeaders(readBuffer)
    }

    @Test
    fun testDumpSimplePacketBlock() {
        dumper = PcapNgFilePacketDumper("/tmp", "test")
        dumper.open()
        val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        dumper.dumpBuffer(buffer, 0, buffer.limit(), false, null)
        dumper.close()
        val readBuffer = readFile()
        verifyHeaders(readBuffer)
        val simplePacketBlock = PcapNgSimplePacketBlock.fromStream(readBuffer)
        assertEquals(buffer, ByteBuffer.wrap(simplePacketBlock.packetData))
    }

    @Test
    fun testDumpSinglePacketBlockWithDummyEth() {
        dumper = PcapNgFilePacketDumper("/tmp", "test")
        dumper.open()
        val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        dumper.dumpBuffer(buffer, 0, buffer.limit(), false, EtherType.IPv4)
        dumper.close()
        val readBuffer = readFile()
        verifyHeaders(readBuffer)
        val simplePacketBlock = PcapNgSimplePacketBlock.fromStream(readBuffer)

        // parse out the ethernet dummy header
        val packetData = ByteBuffer.wrap(simplePacketBlock.packetData)
        EthernetHeader.fromStream(packetData)

        // remaining data in the stream should now match the original buffer
        val recvData = ByteBuffer.allocate(packetData.remaining())
        recvData.put(packetData)
        recvData.rewind()

        assertEquals(buffer, recvData)
    }

    @Test
    fun testMultipleSimplePacketBlocks() {
        dumper = PcapNgFilePacketDumper("/tmp", "test")
        dumper.open()
        val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        val buffer2 = ByteBuffer.wrap(byteArrayOf(0x05, 0x06, 0x07, 0x08))
        dumper.dumpBuffer(buffer, 0, buffer.limit(), false, null)
        dumper.dumpBuffer(buffer2, 0, buffer2.limit(), false, null)
        dumper.close()
        val readBuffer = readFile()
        verifyHeaders(readBuffer)
        val simplePacketBlock = PcapNgSimplePacketBlock.fromStream(readBuffer)
        val simplePacketBlock2 = PcapNgSimplePacketBlock.fromStream(readBuffer)

        assertEquals(buffer, ByteBuffer.wrap(simplePacketBlock.packetData))
        assertEquals(buffer2, ByteBuffer.wrap(simplePacketBlock2.packetData))
    }

    @Test
    fun testDumpEnhancedPacketBlock() {
        dumper = PcapNgFilePacketDumper("/tmp", "test", false)
        dumper.open()
        val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        dumper.dumpBuffer(buffer, 0, buffer.limit(), false, null)
        dumper.close()
        val readBuffer = readFile()
        verifyHeaders(readBuffer)
        val enhancedPacketBlock = PcapNgEnhancedPacketBlock.fromStream(readBuffer)
        assertEquals(buffer, ByteBuffer.wrap(enhancedPacketBlock.packetData))
    }

    @Test
    fun testDumpSingleEnhancedPacketBlockWithDummyEth() {
        dumper = PcapNgFilePacketDumper("/tmp", "test", false)
        dumper.open()
        val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        dumper.dumpBuffer(buffer, 0, buffer.limit(), false, EtherType.IPv4)
        dumper.close()
        val readBuffer = readFile()
        verifyHeaders(readBuffer)
        val enhancedPacketBlock = PcapNgEnhancedPacketBlock.fromStream(readBuffer)

        // parse out the ethernet dummy header
        val packetData = ByteBuffer.wrap(enhancedPacketBlock.packetData)
        EthernetHeader.fromStream(packetData)

        // remaining data in the stream should now match the original buffer
        val recvData = ByteBuffer.allocate(packetData.remaining())
        recvData.put(packetData)
        recvData.rewind()

        assertEquals(buffer, recvData)
    }

    @Test
    fun testDumpMultipleEnhancedPacketBlocks() {
        dumper = PcapNgFilePacketDumper("/tmp", "test", false)
        dumper.open()
        val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        val buffer2 = ByteBuffer.wrap(byteArrayOf(0x05, 0x06, 0x07, 0x08))
        dumper.dumpBuffer(buffer, 0, buffer.limit(), false, null)
        dumper.dumpBuffer(buffer2, 0, buffer2.limit(), false, null)
        dumper.close()
        val readBuffer = readFile()
        verifyHeaders(readBuffer)
        val enhancedPacketBlock = PcapNgEnhancedPacketBlock.fromStream(readBuffer)
        val enhancedPacketBlock2 = PcapNgEnhancedPacketBlock.fromStream(readBuffer)

        assertEquals(buffer, ByteBuffer.wrap(enhancedPacketBlock.packetData))
        assertEquals(buffer2, ByteBuffer.wrap(enhancedPacketBlock2.packetData))
    }
}
