package com.jasonernst.packetdumper.pcapng

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * https://www.ietf.org/archive/id/draft-tuexen-opsawg-pcapng-03.html#name-interface-description-block
 */
class PcapNgInterfaceDescriptionBlock(
    private val linkType: PcapNgLinkType = PcapNgLinkType.ETHERNET,
) : PcapNgBlock {
    companion object {
        // block type (4) + 2x block len (4) + link type (2) + reserved (2) + snap len (4)
        private const val HEADER_BLOCK_LENGTH = 20u

        /**
         * Reads a section header block from a stream, throws a PcapNgException if the block is not a
         * section header live block.
         */
        fun fromStream(stream: ByteBuffer): PcapNgInterfaceDescriptionBlock {
            val startingPosition = stream.position()
            stream.order(ByteOrder.LITTLE_ENDIAN)
            val blockType = stream.int
            if (blockType != PcapNgBlockType.INTERFACE_DESCRIPTION_BLOCK.value.toInt()) {
                throw PcapNgException(
                    "Block type is not an interface description block, expected ${PcapNgBlockType.INTERFACE_DESCRIPTION_BLOCK.value} got $blockType",
                )
            }
            val blockLength = stream.int
            if (blockLength != HEADER_BLOCK_LENGTH.toInt()) {
                throw PcapNgException("Block length is not the expected length")
            }
            val linkType = stream.short
            val reserved = stream.short
            val snapLen = stream.int
            if (snapLen != 0) {
                throw PcapNgException("Snap length is not the expected value")
            }
            val secondBlockLength = stream.int
            if (secondBlockLength != blockLength) {
                throw PcapNgException("Second block length is not the expected value")
            }
            if (stream.position() - startingPosition != HEADER_BLOCK_LENGTH.toInt()) {
                throw PcapNgException(
                    "Stream position is not at the end of the block, expected ${startingPosition + HEADER_BLOCK_LENGTH.toInt()} got ${stream.position()}",
                )
            }
            return PcapNgInterfaceDescriptionBlock(PcapNgLinkType.fromValue(linkType.toUShort()))
        }
    }

    override fun size(): UInt = HEADER_BLOCK_LENGTH

    override fun toBytes(): ByteArray {
        val buffer = ByteBuffer.allocate(HEADER_BLOCK_LENGTH.toInt())
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(PcapNgBlockType.INTERFACE_DESCRIPTION_BLOCK.value.toInt())
        buffer.putInt(HEADER_BLOCK_LENGTH.toInt())

        buffer.putShort(linkType.value.toShort())
        buffer.putShort(0) // reserved
        buffer.putInt(0) // snap length

        // no options

        buffer.putInt(HEADER_BLOCK_LENGTH.toInt())
        return buffer.array()
    }
}
