package com.jasonernst.example_android.nextheader

import com.jasonernst.example_android.ip.IPType
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Common functionality across headers that are encapsulated within an IP packet:
 * - TCP
 * - UDP
 * - ICMPv4
 * - ICMPv6
 */
interface NextHeader {
    companion object {
        fun fromStream(stream: ByteBuffer, protocol: UByte): NextHeader {
            when (protocol) {
                IPType.TCP.value -> {
                    //return TCPHeader.fromStream(stream)
                }
            }
        }
    }

    // return the length of the header, in bytes (not including any payload it might have)
    fun getHeaderLength(): UShort

    // return the header as a byte array (not including any payload it might have)
    fun toByteArray(order: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray

    // Should match the value in the IP header protocol field
    // https://www.iana.org/assignments/protocol-numbers/protocol-numbers.xhtml
    val protocol: UByte

    // Makes it easier to identify the type of header when debugging
    val typeString: String
}