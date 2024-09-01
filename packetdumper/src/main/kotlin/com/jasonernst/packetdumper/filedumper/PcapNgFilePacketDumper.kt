package com.jasonernst.packetdumper.filedumper

import com.jasonernst.packetdumper.ethernet.EtherType
import com.jasonernst.packetdumper.ethernet.EthernetHeader.Companion.prependDummyHeader
import com.jasonernst.packetdumper.pcapng.PcapNgEnhancedPacketBlock
import com.jasonernst.packetdumper.pcapng.PcapNgInterfaceDescriptionBlock
import com.jasonernst.packetdumper.pcapng.PcapNgSectionHeaderBlockLive
import com.jasonernst.packetdumper.pcapng.PcapNgSimplePacketBlock
import java.io.BufferedOutputStream
import java.nio.ByteBuffer

/**
 * Dumps packets to a file in the PCAP-NG format.
 *
 * @param path The path to the file
 * @param name The name of the file
 * @param isSimple If true, the file will be written in the simple format.
 *  If false, it will be written in the enhanced format.
 */
class PcapNgFilePacketDumper(
    path: String,
    name: String,
    private val isSimple: Boolean = true,
) : AbstractFilePacketDumper(path, name, "pcapng") {
    private lateinit var outputStreamWriter: BufferedOutputStream

    override fun open() {
        super.open()
        outputStreamWriter = BufferedOutputStream(file.outputStream())
        outputStreamWriter.write(PcapNgSectionHeaderBlockLive.toBytes())
        outputStreamWriter.flush()
        outputStreamWriter.write(PcapNgInterfaceDescriptionBlock().toBytes())
        outputStreamWriter.flush()
    }

    override fun close() {
        super.close()
        outputStreamWriter.flush()
        outputStreamWriter.close()
    }

    /**
     * Writes a packet to the file. In this case, the address parameter is ignored.
     */
    override fun dumpBuffer(
        buffer: ByteBuffer,
        offset: Int,
        length: Int,
        addresses: Boolean,
        etherType: EtherType?,
    ) {
        val startingPosition = buffer.position()
        val originalLimit = buffer.limit()
        // optionally prepend the ethernet dummy header
        val conversionBuffer =
            if (etherType != null) {
                prependDummyHeader(buffer, offset, length, etherType)
            } else {
                val actualLimit = minOf(originalLimit, offset + length)
                buffer.limit(actualLimit)
            }

        if (isSimple) {
            val packetBlock = PcapNgSimplePacketBlock(conversionBuffer.array())
            outputStreamWriter.write(packetBlock.toBytes())
            outputStreamWriter.flush()
        } else {
            val timestamp = System.currentTimeMillis()
            val packetBlock = PcapNgEnhancedPacketBlock(conversionBuffer.array(), timestamp = timestamp)
            outputStreamWriter.write(packetBlock.toBytes())
            outputStreamWriter.flush()
        }
        buffer.position(startingPosition)
        buffer.limit(originalLimit)
    }
}
