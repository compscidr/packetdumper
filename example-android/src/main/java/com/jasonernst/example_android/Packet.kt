package com.jasonernst.example_android

import com.jasonernst.example_android.ip.IPHeader
import com.jasonernst.example_android.nextheader.NextHeader
import java.nio.ByteBuffer

/**
 * Encapsulates everything we need a for a full packet, an IP header, a set of next headers (usually
 * only a single next header if we have an IPv4 packet, but could be more if we have an IPv6 packet
 * with hop-by-hop options, for example).
 */
data class Packet(val ipHeader: IPHeader, val nextHeaders: NextHeader, val payload: ByteArray) {

    companion object {
        fun fromStream(stream: ByteBuffer): Packet {
            val ipHeader = IPHeader.fromStream(stream)
            val nextHeader = NextHeader.fromStream(stream, ipHeader.protocol)

            if (stream.remaining() < ipHeader.getPayloadLength()) {
                throw PacketTooShortException("Packet too short to obtain entire payload")
            }
            val payload = ByteArray(ipHeader.getPayloadLength())
            stream.get(payload)
            return Packet(ipHeader, nextHeader, payload)
        }
    }

    fun toByteArray(): ByteArray {
        val buffer = ByteBuffer.allocate(ipHeader.getTotalLength())
        buffer.put(ipHeader.toByteArray())
        buffer.put(nextHeaders.toByteArray())
        buffer.put(payload)
        return buffer.array()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Packet

        if (ipHeader != other.ipHeader) return false
        if (nextHeaders != other.nextHeaders) return false
        if (!payload.contentEquals(other.payload)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ipHeader.hashCode()
        result = 31 * result + nextHeaders.hashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }
}