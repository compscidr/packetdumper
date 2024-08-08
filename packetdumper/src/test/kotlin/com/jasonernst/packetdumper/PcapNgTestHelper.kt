package com.jasonernst.packetdumper

import com.jasonernst.packetdumper.filedumper.AbstractFilePacketDumper
import com.jasonernst.packetdumper.pcapng.PcapNgBlock
import com.jasonernst.packetdumper.pcapng.PcapNgInterfaceDescriptionBlock
import com.jasonernst.packetdumper.pcapng.PcapNgSectionHeaderBlockLive
import com.jasonernst.packetdumper.stringdumper.StringPacketDumper
import org.slf4j.LoggerFactory
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.nio.ByteBuffer

object PcapNgTestHelper {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Verify that the file has the correct headers, advances the readBuffer beyond these headers
     * and returns a list of the blocks.
     */
    fun verifyHeaders(readBuffer: ByteBuffer): List<PcapNgBlock> {
        val pcapBlocks = mutableListOf<PcapNgBlock>()

        // we expect the file to start with a section header block
        pcapBlocks.add(PcapNgSectionHeaderBlockLive.fromStream(readBuffer))

        // we expect the file to have an interface description block
        pcapBlocks.add(PcapNgInterfaceDescriptionBlock.fromStream(readBuffer))

        return pcapBlocks
    }

    fun readFile(dumper: AbstractFilePacketDumper): ByteBuffer {
        val readBuffer = ByteBuffer.wrap(BufferedInputStream(FileInputStream(dumper.filename)).readAllBytes())
        val stringPacketDumper = StringPacketDumper(logger)
        stringPacketDumper.dumpBuffer(readBuffer, 0, readBuffer.limit(), false, null)
        return readBuffer
    }
}
