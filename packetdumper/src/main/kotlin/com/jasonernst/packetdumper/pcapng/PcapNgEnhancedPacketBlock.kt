package com.jasonernst.packetdumper.pcapng

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Implements:
 * https://www.ietf.org/archive/id/draft-tuexen-opsawg-pcapng-03.html#name-enhanced-packet-block
 * @param packetData The packet data
 * @param timestamp The timestamp of the packet. The units are set in the interface description block
 *   but regardless of the unit it starts from 1970-01-01 00:00:00 UTC
 */
class PcapNgEnhancedPacketBlock(
    val packetData: ByteArray,
    private val originalPacketLength: Int = packetData.size,
    private val timestamp: Long = 0,
) : PcapNgBlock {
    companion object {
        // block type (4) + 2 * block len (4) + interface id (4) + timestamp high (4) + timestamp low (4) + captured length (4) + original length (4)
        const val HEADER_BLOCK_LENGTH_WITHOUT_PACKET = 32u

        fun fromStream(stream: ByteBuffer): PcapNgEnhancedPacketBlock {
            stream.order(ByteOrder.LITTLE_ENDIAN)
            val blockType = stream.int
            if (blockType != PcapNgBlockType.ENHANCED_PACKET_BLOCK.value.toInt()) {
                throw PcapNgException(
                    "Block type is not an enhanced packet block, expected ${PcapNgBlockType.ENHANCED_PACKET_BLOCK.value} got $blockType",
                )
            }
            val blockLength = stream.int
            val interfaceId = stream.int
            val timestampHigh = stream.int.toLong()
            val timestampLow = stream.int.toLong()
            val capturedLength = stream.int
            val originalLength = stream.int
            val packetData = ByteArray(capturedLength)
            stream.get(packetData)
            val zeroPad = blockLength - HEADER_BLOCK_LENGTH_WITHOUT_PACKET.toInt() - capturedLength
            stream.position(stream.position() + zeroPad)
            val trailer = stream.int
            if (trailer != blockLength) {
                throw PcapNgException("Trailer is not the expected value")
            }
            val timestamp = (timestampHigh shl 32) or timestampLow
            return PcapNgEnhancedPacketBlock(packetData, originalLength, timestamp)
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
        buffer.putInt(PcapNgBlockType.ENHANCED_PACKET_BLOCK.value.toInt())
        buffer.putInt(size().toInt())
        buffer.putInt(0) // interface id

        // timestamp
        buffer.putInt((timestamp shr 32).toInt())
        buffer.putInt(timestamp.toInt())

        buffer.putInt(packetData.size)
        buffer.putInt(originalPacketLength)

        // packet data
        buffer.put(packetData)
        // zero pad
        for (i in 0 until zeroPadSize()) {
            buffer.put(0)
        }
        buffer.putInt(size().toInt())

        return buffer.array()
    }
}
