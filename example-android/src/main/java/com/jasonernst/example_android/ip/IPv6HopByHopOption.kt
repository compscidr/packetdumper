package com.jasonernst.example_android.ip

import com.jasonernst.example_android.PacketTooShortException
import java.nio.ByteBuffer

/**
 * The length is measured in 8-octet units, not including the first 8 octets:
 * https://www.rfc-editor.org/rfc/rfc8200#page-13
 */
class IPv6HopByHopOption(nextHeader: UByte, length: UByte, data: ByteArray): IPv6ExtensionHeader(nextHeader, length, data) {
    companion object {
        private val logger = org.slf4j.LoggerFactory.getLogger(IPv6HopByHopOption::class.java)
        const val MIN_HEADER_LENGTH = 8u

        /**
         * Attempts to parse an Ipv6 hop by hop extension header. Assumes the stream points to the start
         * of the extension header and that it is in fact a hop by hop extension header.
         *
         * If there are not enough bytes remaining in the stream, a PacketHeaderException is thrown.
         * It is up to the callee to keep track of the start of the IP header and rewind to the buffer
         * to the start in the case there aren't enough bytes read at this point yet.
         */
        fun fromStream(stream: ByteBuffer): IPv6HopByHopOption {
            logger.debug("Ipv6 Hop-by-hop Starting position: ${stream.position()}")
            val nextHeader = stream.get().toUByte()
            val nextHeaderType =
                try {
                    IPType.fromValue(nextHeader)
                } catch (e: Exception) {
                    "Unknown Ipv6 hop-by-hop next header type"
                }
            logger.debug("Ipv6 Hop-by-hop next header type: $nextHeader $nextHeaderType")
            val headerLength = stream.get().toUByte()
            logger.debug("Ipv6 hop-by-hop header length: $headerLength")

            // since we've already parsed out the next header type (1 byte) and the ipv6 hop-by-hop header length
            // we remove two. The length is the count of 8 octet units, not counting the first 8 octets: https://datatracker.ietf.org/doc/html/rfc2460#page-11
            val expectedRemaining = IPv6HopByHopOption.MIN_HEADER_LENGTH.toInt() - 2 + (8 * headerLength.toInt())
            if (stream.remaining() < expectedRemaining) {
                throw PacketTooShortException("IPv6 hop by hop expecting $expectedRemaining bytes but only have ${stream.remaining()}")
            }
            val optionData = ByteArray(expectedRemaining)
            stream.get(optionData)
            logger.debug("Ipv6 Hop-by-hop Ending position: ${stream.position()}")
            return IPv6HopByHopOption(nextHeader, headerLength, optionData)
        }
    }

    fun getTotalLength(): UByte {
        return (MIN_HEADER_LENGTH + (8u * length)).toUByte()
    }

    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(MIN_HEADER_LENGTH.toInt() + (8 * length.toInt()))
        buffer.put(nextHeader.toByte())
        buffer.put(length.toByte())
        buffer.put(data)
        return buffer.array()
    }
}