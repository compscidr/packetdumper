package com.jasonernst.packetdumper.serverdumper

import com.jasonernst.packetdumper.PcapNgTestHelper.verifyHeaders
import com.jasonernst.packetdumper.ethernet.EtherType
import com.jasonernst.packetdumper.ethernet.EthernetHeader
import com.jasonernst.packetdumper.pcapng.PcapNgEnhancedPacketBlock
import com.jasonernst.packetdumper.pcapng.PcapNgSimplePacketBlock
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer

class TestPcapNgTcpServerPacketDumper {
    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var dumper: PcapNgTcpServerPacketDumper
    private var tcpClientSocket: Socket? = null

    private fun connectToServer(server: InetSocketAddress) {
        tcpClientSocket = Socket()
        tcpClientSocket?.connect(server, 1000)
    }

    private fun disconnect() {
        try {
            tcpClientSocket?.close()
        } catch (e: Exception) {
            // ignore
        }
    }

    @AfterEach
    fun tearDown() {
        // just in case a test fails, make sure we clean up
        dumper.stop()
        disconnect()
    }

    @Test
    fun startStop() {
        dumper = PcapNgTcpServerPacketDumper()
        dumper.start()
        dumper.stop()
    }

    private fun waitForData(): ByteBuffer {
        logger.debug("Waiting to receive data from the tcp dumper")
        var available = 0
        do {
            available = tcpClientSocket?.getInputStream()?.available() ?: 0
            Thread.sleep(100)
        } while (available == 0)
        logger.debug("Available bytes: $available")

        val recvBuffer = ByteArray(available)
        var bytesRead = 0
        do {
            bytesRead += tcpClientSocket?.getInputStream()?.read(recvBuffer) ?: 0
            Thread.sleep(100)
        } while (bytesRead < available)
        logger.debug("Received $bytesRead bytes from the tcp dumper")
        return ByteBuffer.wrap(recvBuffer)
    }

    /**
     * Test that the server can be started and stopped with a client connected.
     *
     * Also tests that the client receives the two required header blocks upon connection.
     */
    @Test
    fun startStopWithClient() {
        dumper = PcapNgTcpServerPacketDumper()
        dumper.start()
        connectToServer(InetSocketAddress("localhost", PcapNgTcpServerPacketDumper.DEFAULT_PORT))

        val readBuffer = waitForData()
        verifyHeaders(readBuffer)

        dumper.stop()
    }

    @Test
    fun testDumpSimplePacketBlock() {
        dumper = PcapNgTcpServerPacketDumper(isSimple = true)
        dumper.start()
        connectToServer(InetSocketAddress("localhost", PcapNgTcpServerPacketDumper.DEFAULT_PORT))

        var readBuffer = waitForData()
        verifyHeaders(readBuffer)

        val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        dumper.dumpBuffer(buffer, 0, buffer.limit(), false, null)

        readBuffer = waitForData()

        val simplePacketBlock = PcapNgSimplePacketBlock.fromStream(readBuffer)
        assertEquals(buffer, ByteBuffer.wrap(simplePacketBlock.packetData))
    }

    @Test
    fun testDumpSinglePacketBlockWithDummyEth() {
        dumper = PcapNgTcpServerPacketDumper(isSimple = true)
        dumper.start()
        connectToServer(InetSocketAddress("localhost", PcapNgTcpServerPacketDumper.DEFAULT_PORT))

        var readBuffer = waitForData()
        verifyHeaders(readBuffer)

        val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        dumper.dumpBuffer(buffer, 0, buffer.limit(), false, EtherType.IPv4)

        readBuffer = waitForData()

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
        dumper = PcapNgTcpServerPacketDumper(isSimple = true)
        dumper.start()
        connectToServer(InetSocketAddress("localhost", PcapNgTcpServerPacketDumper.DEFAULT_PORT))

        var readBuffer = waitForData()
        verifyHeaders(readBuffer)

        val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        val buffer2 = ByteBuffer.wrap(byteArrayOf(0x05, 0x06, 0x07, 0x08))
        dumper.dumpBuffer(buffer, 0, buffer.limit(), false, null)
        dumper.dumpBuffer(buffer2, 0, buffer2.limit(), false, null)

        readBuffer = waitForData()

        val simplePacketBlock = PcapNgSimplePacketBlock.fromStream(readBuffer)
        assertEquals(buffer, ByteBuffer.wrap(simplePacketBlock.packetData))

        if (readBuffer.hasRemaining()) {
            val simplePacketBlock2 = PcapNgSimplePacketBlock.fromStream(readBuffer)
            assertEquals(buffer2, ByteBuffer.wrap(simplePacketBlock2.packetData))
        } else {
            readBuffer = waitForData()
            val simplePacketBlock2 = PcapNgSimplePacketBlock.fromStream(readBuffer)
            assertEquals(buffer2, ByteBuffer.wrap(simplePacketBlock2.packetData))
        }
    }

    @Test
    fun testDumpEnhancedPacketBlock() {
        dumper = PcapNgTcpServerPacketDumper(isSimple = false)
        dumper.start()
        connectToServer(InetSocketAddress("localhost", PcapNgTcpServerPacketDumper.DEFAULT_PORT))

        var readBuffer = waitForData()
        verifyHeaders(readBuffer)

        val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        dumper.dumpBuffer(buffer, 0, buffer.limit(), false, null)

        readBuffer = waitForData()

        val enhancedPacketBlock = PcapNgEnhancedPacketBlock.fromStream(readBuffer)
        assertEquals(buffer, ByteBuffer.wrap(enhancedPacketBlock.packetData))
    }

    @Test
    fun testDumpSingleEnhancedPacketBlockWithDummyEth() {
        dumper = PcapNgTcpServerPacketDumper(isSimple = false)
        dumper.start()
        connectToServer(InetSocketAddress("localhost", PcapNgTcpServerPacketDumper.DEFAULT_PORT))

        var readBuffer = waitForData()
        verifyHeaders(readBuffer)

        val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        dumper.dumpBuffer(buffer, 0, buffer.limit(), false, EtherType.IPv4)

        readBuffer = waitForData()

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
        dumper = PcapNgTcpServerPacketDumper(isSimple = false)
        dumper.start()
        connectToServer(InetSocketAddress("localhost", PcapNgTcpServerPacketDumper.DEFAULT_PORT))

        var readBuffer = waitForData()
        verifyHeaders(readBuffer)

        val buffer = ByteBuffer.wrap(byteArrayOf(0x01, 0x02, 0x03, 0x04))
        val buffer2 = ByteBuffer.wrap(byteArrayOf(0x05, 0x06, 0x07, 0x08))
        dumper.dumpBuffer(buffer, 0, buffer.limit(), false, null)
        dumper.dumpBuffer(buffer2, 0, buffer2.limit(), false, null)

        readBuffer = waitForData()

        val enhancedPacketBlock = PcapNgEnhancedPacketBlock.fromStream(readBuffer)
        assertEquals(buffer, ByteBuffer.wrap(enhancedPacketBlock.packetData))

        if (readBuffer.hasRemaining()) {
            val enhancedPacketBlock2 = PcapNgEnhancedPacketBlock.fromStream(readBuffer)
            assertEquals(buffer2, ByteBuffer.wrap(enhancedPacketBlock2.packetData))
        } else {
            readBuffer = waitForData()
            val enhancedPacketBlock2 = PcapNgSimplePacketBlock.fromStream(readBuffer)
            assertEquals(buffer2, ByteBuffer.wrap(enhancedPacketBlock2.packetData))
        }
    }
}
