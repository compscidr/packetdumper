package com.jasonernst.example_android.ip

import com.jasonernst.example_android.PacketTooShortException
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * Collects up the common things between IPv4 and IPv6 headers.
 */
interface IPHeader {
    companion object {
        private val logger = LoggerFactory.getLogger(IPHeader::class.java)
        const val IP4_VERSION: UByte = 4u
        const val IP6_VERSION: UByte = 6u

        fun fromStream(stream: ByteBuffer): IPHeader {
            if (stream.remaining() < 1) {
                throw PacketTooShortException("Packet too short to determine type")
            }
            // version is in the top four bits of the first byte, so we need to shift and zero out
            // the bottom four bits
            val versionByte = stream.get()
            return when (val version = (versionByte.toInt() shr 4 and 0x0F).toUByte()) {
                IP4_VERSION -> {
                    logger.debug("IPv4 packet")
                    IPv4Header.fromStream(stream)
                }
                IP6_VERSION -> {
                    logger.debug("IPv6 packet")
                    IPv6Header.fromStream(stream)
                }
                else -> {
                    throw IllegalArgumentException("Unknown packet type: $version")
                }
            }
        }
    }

    // ipv4 or ipv6
    val version: UByte

    // 8-bits, Next-layer protocol (TCP, UDP, ICMP, etc)
    // from this list: https://en.wikipedia.org/wiki/List_of_IP_protocol_numbers
    val protocol: UByte

    var sourceAddress: InetAddress
    var destinationAddress: InetAddress

    fun toByteArray(): ByteArray

    // return the length of the IP packet, including the header, extension headers (if necessary)
    // and payload
    fun getTotalLength(): Int

    // get the length of just the payload
    fun getPayloadLength(): Int
}