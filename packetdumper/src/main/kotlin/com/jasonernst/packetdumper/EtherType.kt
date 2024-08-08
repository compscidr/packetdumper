package com.jasonernst.packetdumper

/**
 * https://www.iana.org/assignments/ieee-802-numbers/ieee-802-numbers.xhtml#ieee-802-numbers-1
 * https://en.wikipedia.org/wiki/EtherType
 */
enum class EtherType(
    val value: UShort,
) {
    BUMP(0x0101u),
    IPv4(0x0800u),
    ARP(0x0806u),
    IPv6(0x86DDu),
    DETECT(0xFFFFu), // used if we want to detect the type from the packet itself
    ;

    companion object {
        fun fromValue(value: UShort) = entries.first { it.value == value }
    }
}
