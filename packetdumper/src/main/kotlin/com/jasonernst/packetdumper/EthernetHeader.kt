package com.jasonernst.packetdumper

import org.slf4j.LoggerFactory
import java.nio.ByteBuffer

/**
 * Bare minimal Ethernet frame.
 *
 * Mostly used to help us easily add a dummy header to packet dumps for Wireshark.
 *
 * https://en.wikipedia.org/wiki/Ethernet_frame
 */
class EthernetHeader(private var destination: MACAddress, private var source: MACAddress, private var type: EtherType) {
    companion object {
        const val IP4_VERSION: UByte = 4u
        const val IP6_VERSION: UByte = 6u
        const val ETHERNET_HEADER_LENGTH = 14u
        private val logger = LoggerFactory.getLogger(this::class.java)

        /**
         * Returns a new ByteBuffer with the Ethernet header prepended to the front of the buffer.
         *
         * The buffer is copied starting from offset continuing for length bytes, or until the end of
         * the buffer if length + offset > buffer.limit(). The source and destination addresses are
         */
        fun prependDummyHeader(
            buffer: ByteBuffer,
            offset: Int = 0,
            length: Int = 0,
            etherType: EtherType,
        ): ByteBuffer {
            val startingPosition = buffer.position()
            val totalLength = minOf(length, buffer.limit() - offset)
            if (totalLength < length) {
                logger.warn("Trying to dump more bytes than are in the buffer. Dumping up to buffer limit.")
            }
            val newBuffer = ByteBuffer.allocate(totalLength + ETHERNET_HEADER_LENGTH.toInt())
            val detectedEtherType =
                if (etherType == EtherType.DETECT) {
                    val ipVersion = buffer.get(offset).toUByte()
                    buffer.position(startingPosition)
                    getEtherTypeFromIPVersionByte(ipVersion)
                } else {
                    etherType
                }
            newBuffer.put(dummyEthernet(detectedEtherType).toBytes())
            newBuffer.put(buffer.array(), offset, totalLength)
            newBuffer.rewind()
            return newBuffer
        }

        fun fromStream(stream: ByteBuffer): EthernetHeader {
            val destination = MACAddress.fromStream(stream)
            val source = MACAddress.fromStream(stream)
            val type = EtherType.fromValue(stream.short.toUShort())
            return EthernetHeader(destination, source, type)
        }

        /**
         * Attempts to detect the ethertype based on the byte passed to it. If its not a known
         * ethertype, it will just leave the type as 0xFFFF (detect) which maps to RESERVED in the
         * actual mapping.
         */
        fun getEtherTypeFromIPVersionByte(ipVersion: UByte): EtherType {
            return when (ipVersion) {
                IP4_VERSION -> EtherType.IPv4
                IP6_VERSION -> EtherType.IPv6
                else -> {
                    logger.warn("Couldn't detect etherType, got $ipVersion")
                    EtherType.DETECT
                }
            }
        }

        fun dummyEthernet(etherType: EtherType): EthernetHeader {
            return EthernetHeader(MACAddress.DUMMY_MAC_SOURCE, MACAddress.DUMMY_MAC_DEST, etherType)
        }
    }

    fun size() = ETHERNET_HEADER_LENGTH

    fun toBytes(): ByteArray {
        val buffer = ByteBuffer.allocate(ETHERNET_HEADER_LENGTH.toInt())
        buffer.put(destination.bytes)
        buffer.put(source.bytes)
        buffer.putShort(type.value.toShort())
        return buffer.array()
    }

    override fun toString(): String {
        return "EthernetHeader(destination=$destination, source=$source, type=$type, size=${size()})"
    }
}
