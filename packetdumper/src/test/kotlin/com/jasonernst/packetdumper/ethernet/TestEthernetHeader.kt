package com.jasonernst.packetdumper.ethernet

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class TestEthernetHeader {
    @Test fun serialization() {
        val header = EthernetHeader.dummyEthernet(EtherType.IPv4)
        val buffer = ByteBuffer.wrap(header.toBytes())
        val parsedHeader = EthernetHeader.fromStream(buffer)
        assertEquals(header, parsedHeader)
        assertEquals(parsedHeader.size(), EthernetHeader.ETHERNET_HEADER_LENGTH)
    }

    @Test fun etherTypeDetection() {
        val ipv4Buffer = ByteBuffer.allocate(1)
        // need to shift left because its the high 4 bits of the byte that contain the version
        ipv4Buffer.put(0, (EthernetHeader.IP4_VERSION.toUInt() shl 4).toByte())
        ipv4Buffer.rewind()
        assertEquals(EtherType.IPv4, EthernetHeader.getEtherTypeFromIPVersionByte(ipv4Buffer.get().toUByte()))

        val ipv6Buffer = ByteBuffer.allocate(1)
        // need to shift left because its the high 4 bits of the byte that contain the version
        ipv6Buffer.put(0, (EthernetHeader.IP6_VERSION.toUInt() shl 4).toByte())
        ipv6Buffer.rewind()
        assertEquals(EtherType.IPv6, EthernetHeader.getEtherTypeFromIPVersionByte(ipv6Buffer.get().toUByte()))

        val unknownBuffer = ByteBuffer.allocate(1)
        unknownBuffer.put(0, 0.toByte())
        unknownBuffer.rewind()
        assertEquals(EtherType.DETECT, EthernetHeader.getEtherTypeFromIPVersionByte(unknownBuffer.get().toUByte()))
    }
}
