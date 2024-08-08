package com.jasonernst.packetdumper.pcapng

import java.nio.ByteBuffer
import java.nio.ByteOrder

object PcapNgSectionHeaderBlockLive : PcapNgBlock {
    private const val ENDIAN_MAGIC = 0x1A2B3C4D
    private const val MAJOR_VERSION: UShort = 1U
    private const val MINOR_VERSION: UShort = 0U
    private const val SECTION_LENGTH = -1L // since we are doing a live capture, we don't know the length

    // block type (4) + 2x block len (4) + magic (4) + major (2) + minor (2) + section len (8)
    private const val HEADER_BLOCK_LENGTH = 28u

    override fun size(): UInt = HEADER_BLOCK_LENGTH

    override fun toBytes(): ByteArray {
        val buffer = ByteBuffer.allocate(HEADER_BLOCK_LENGTH.toInt())
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(PcapNgBlockType.SECTION_HEADER_BLOCK.value.toInt())
        buffer.putInt(HEADER_BLOCK_LENGTH.toInt())
        buffer.putInt(ENDIAN_MAGIC)
        buffer.putShort(MAJOR_VERSION.toShort())
        buffer.putShort(MINOR_VERSION.toShort())
        buffer.putLong(SECTION_LENGTH)
        buffer.putInt(HEADER_BLOCK_LENGTH.toInt())
        return buffer.array()
    }

    /**
     * Reads a section header block from a stream, throws a PcapNgException if the block is not a
     * section header live block.
     */
    fun fromStream(stream: ByteBuffer): PcapNgSectionHeaderBlockLive {
        val startPosition = stream.position()
        stream.order(ByteOrder.LITTLE_ENDIAN)
        val blockType = stream.int
        if (blockType != PcapNgBlockType.SECTION_HEADER_BLOCK.value.toInt()) {
            throw PcapNgException(
                "Block type is not a section header block, expected ${PcapNgBlockType.SECTION_HEADER_BLOCK.value} got $blockType",
            )
        }
        val blockLength = stream.int
        if (blockLength != HEADER_BLOCK_LENGTH.toInt()) {
            throw PcapNgException("Block length is not the expected length")
        }
        val endianMagic = stream.int
        if (endianMagic != ENDIAN_MAGIC) {
            throw PcapNgException("Endian magic is not the expected value")
        }
        val majorVersion = stream.short.toUShort()
        if (majorVersion != MAJOR_VERSION) {
            throw PcapNgException("Major version is not the expected value")
        }
        val minorVersion = stream.short.toUShort()
        if (minorVersion != MINOR_VERSION) {
            throw PcapNgException("Minor version is not the expected value")
        }
        val sectionLength = stream.long
        if (sectionLength != SECTION_LENGTH) {
            throw PcapNgException("Section length is not the expected value")
        }
        val blockTotalLength = stream.int
        if (blockTotalLength != HEADER_BLOCK_LENGTH.toInt()) {
            throw PcapNgException("Block total length is not the expected value")
        }
        if (stream.position() - startPosition != HEADER_BLOCK_LENGTH.toInt()) {
            throw PcapNgException(
                "Stream position is not at the end of the block, expected ${startPosition + HEADER_BLOCK_LENGTH.toInt()} got ${stream.position()}",
            )
        }
        return PcapNgSectionHeaderBlockLive
    }
}
