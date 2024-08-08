package com.jasonernst.packetdumper.pcapng

import com.jasonernst.packetdumper.stringdumper.StringPacketDumper
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Implements:
 * https://www.ietf.org/archive/id/draft-tuexen-opsawg-pcapng-03.html#name-simple-packet-block
 */
class PcapNgSimplePacketBlock(
    val packetData: ByteArray,
) : PcapNgBlock {
    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)

        // block type (4) + 2x block len (4) + orig length (4)
        const val HEADER_BLOCK_LENGTH_WITHOUT_PACKET = 16u
        const val PRE_HEADER = 8u
        const val TRAILER = 4u

        /**
         * Reads a simple packet block from a stream, throws a PcapNgException if the block is not
         * a simple packet block. The stream should be positioned at the start of the block. By
         * the end of the function, the stream will be positioned at the start of the next block.
         */
        fun fromStream(stream: ByteBuffer): PcapNgSimplePacketBlock {
            val stringPacketDumper = StringPacketDumper(logger)
            stringPacketDumper.dumpBuffer(stream, stream.position(), stream.remaining())
            stream.order(ByteOrder.LITTLE_ENDIAN)
            val blockType = stream.int
            if (blockType != PcapNgBlockType.SIMPLE_PACKET_BLOCK.value.toInt()) {
                throw PcapNgException(
                    "Block type is not a simple packet block, expected ${PcapNgBlockType.SIMPLE_PACKET_BLOCK.value} got $blockType",
                )
            }
            val blockLength = stream.int
            val packetLength = stream.int
            val packetData = ByteArray(packetLength)
            stream.get(packetData)
            val zeroPad = blockLength - HEADER_BLOCK_LENGTH_WITHOUT_PACKET.toInt() - packetLength
            stream.position(stream.position() + zeroPad)
            val trailer = stream.int
            if (trailer != blockLength) {
                throw PcapNgException("Trailer is not the expected value")
            }
            return PcapNgSimplePacketBlock(packetData)
        }
    }

    /**
     * The size of the block is the size of the packet data rounded up to the nearest 4 bytes, plus
     * the header and trailer
     */
    override fun size(): UInt {
        val nearest4 = 4u * (ceil(abs(packetData.size / 4.0))).toUInt()
        return HEADER_BLOCK_LENGTH_WITHOUT_PACKET + nearest4
    }

    private fun zeroPadSize(): Int {
        val nearest4 = 4 * (ceil(abs(packetData.size / 4.0))).toInt()
        return nearest4 - packetData.size
    }

    override fun toBytes(): ByteArray {
        val buffer = ByteBuffer.allocate(size().toInt())
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(PcapNgBlockType.SIMPLE_PACKET_BLOCK.value.toInt())
        buffer.putInt(size().toInt())
        buffer.putInt(packetData.size)
        buffer.put(packetData)
        // zero pad
        for (i in 0 until zeroPadSize()) {
            buffer.put(0)
        }
        buffer.putInt(size().toInt())
        return buffer.array()
    }
}
