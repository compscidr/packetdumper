package com.jasonernst.packetdumper.pcapng

// https://www.ietf.org/staging/draft-tuexen-opsawg-pcapng-02.html#name-block-types
enum class PcapNgBlockType(
    val value: UInt,
) {
    INTERFACE_DESCRIPTION_BLOCK(1u),
    SIMPLE_PACKET_BLOCK(3u),
    NAME_RESOLUTION_BLOCK(4u),
    INTERFACE_STATISTICS_BLOCK(5u),
    ENHANCED_PACKET_BLOCK(6u),
    SECTION_HEADER_BLOCK(0x0A0D0D0AU),
    ;

    companion object {
        fun fromValue(value: UInt) = entries.first { it.value == value }
    }
}
