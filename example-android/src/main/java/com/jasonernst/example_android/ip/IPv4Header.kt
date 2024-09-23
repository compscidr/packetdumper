package com.jasonernst.example_android.ip

import com.jasonernst.example_android.PacketTooShortException
import com.jasonernst.example_android.ip.IPHeader.Companion.IP4_VERSION
import org.slf4j.LoggerFactory
import java.net.Inet4Address
import java.net.InetAddress
import java.nio.ByteBuffer

/**
 * Internet Protocol Version 4 Header Implementation.
 */
class IPv4Header(
    // 4-bits, should always be IP4_VERSION for an ipv4 packet.
    override val version: UByte = IP4_VERSION,
    // 4-bits, header size. Offset to start of data. This value x IP4_WORD_LENGTH is the header
    // length. Increases when we add options.
    var ihl: UByte,
    // 6-bits, differentiated services code point.
    var dscp: UByte = 0u,
    // 2-bits, explicit congestion notification.
    var ecn: UByte = 0u,
    // 16-bits, total length of the datagram.
    private var totalLength: UShort = 0u,
    // 16-bits, groups fragments of a single IPv4 datagram.
    var id: UShort = 0u,
    // if the packet is marked as don't fragment and we can't fit it in a single packet, drop it.
    var dontFragment: Boolean = true,
    // indicates if this is the last fragment of a larger IPv4 packet.
    var lastFragment: Boolean = true,
    // 13-bits, offset of this fragment from the start of the original packet.
    var fragmentOffset: UShort = 0u,
    // 8-bits, maximum time (hops) the packet is allowed to exist in the internet system.
    // decremented each time the packet passes through a router.
    var ttl: UByte = 64u,
    // 8-bits, Next-layer protocol (TCP, UDP, ICMP, etc)
    // from this list: https://en.wikipedia.org/wiki/List_of_IP_protocol_numbers
    override val protocol: UByte,
    // 16-bits, one's complement of the one's complement sum of the entire header
    // (does not include the payload)
    // https://en.wikipedia.org/wiki/IPv4#Header_checksum
    var headerChecksum: UShort = 0u,
    // 32-bits, source address
    override var sourceAddress: InetAddress,
    // 32-bits, destination address
    override var destinationAddress: InetAddress,
) : IPHeader {

    companion object {
        private val logger = LoggerFactory.getLogger(IPv4Header::class.java)

        fun fromStream(stream: ByteBuffer): IPv4Header {
            val start = stream.position()
            // logger.debug("Parsing IPv4 header from position: $start. remaining: ${stream.remaining()}, limit: ${stream.limit()}")

            // ensure we can get the version
            if (stream.remaining() < 1) {
                throw PacketTooShortException("IPv4Header: stream too short to determine version")
            }

            // ensure we have an IPv4 packet
            val versionAndHeaderLength = stream.get().toUByte()
            val ipVersion = (versionAndHeaderLength.toInt() shr 4 and 0x0F).toUByte()
            if (ipVersion != IP4_VERSION) {
                throw IllegalArgumentException("Invalid IPv4 header. IP version should be 4 but was $ipVersion")
            }

            // ensure we have enough to to get IHL
            if (stream.remaining() < 1) {
                throw IllegalArgumentException("IPv4Header: stream too short to determine header length")
            }

            // ensure we have enough capacity in the stream to parse out a full header
            val ihl: UByte = (versionAndHeaderLength.toInt() and 0x0F).toUByte()
            val headerAvailable = stream.limit() - start
            if (headerAvailable < (ihl * 4u).toInt()) {
                throw PacketTooShortException("Not enough space in stream for IPv4 header, expected ${ihl * 4u} but only have $headerAvailable")
            }

            val dscpAndEcn = stream.get().toUByte()
            val dscp: UByte = (dscpAndEcn.toInt() shr 2 and 0x3F).toUByte()
            val ecn: UByte = (dscpAndEcn.toInt() and 0x03).toUByte()
            val totalLength = stream.short.toUShort()
            val id = stream.short.toUShort()
            val flagsAndFragmentOffset = stream.short.toUShort()
            val dontFragment = flagsAndFragmentOffset.toInt() and 0x4000 != 0
            val lastFragment = flagsAndFragmentOffset.toInt() and 0x2000 != 0
            val fragmentOffset: UShort = (flagsAndFragmentOffset.toInt() and 0x1FFF).toUShort()
            val ttl = stream.get().toUByte()
            val protocol = stream.get().toUByte()
            val checksum = stream.short.toUShort()

            val source = ByteArray(4)
            stream[source]
            val sourceAddress = Inet4Address.getByAddress(source) as Inet4Address
            val destination = ByteArray(4)
            stream[destination]
            val destinationAddress = Inet4Address.getByAddress(destination) as Inet4Address

            // todo (compscidr): parse the options field instead of just dropping them
            if (ihl > 5u) {
                // drop the IP option
                for (i in 0u until (ihl - 5u)) {
                    stream.int
                }
            }

            return IPv4Header(
                ihl = ihl,
                dscp = dscp,
                ecn = ecn,
                totalLength = totalLength,
                id = id,
                dontFragment = dontFragment,
                lastFragment = lastFragment,
                fragmentOffset = fragmentOffset,
                ttl = ttl,
                protocol = protocol,
                headerChecksum = checksum,
                sourceAddress = sourceAddress,
                destinationAddress = destinationAddress,
            )
        }
    }

    override fun toByteArray(): ByteArray {
        TODO("Not yet implemented")
    }

    override fun getTotalLength(): Int {
        TODO("Not yet implemented")
    }

    override fun getPayloadLength(): Int {
        TODO("Not yet implemented")
    }
}