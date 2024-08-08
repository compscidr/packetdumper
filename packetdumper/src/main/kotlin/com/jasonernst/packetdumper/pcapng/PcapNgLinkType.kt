package com.jasonernst.packetdumper.pcapng

// https://www.tcpdump.org/linktypes.html
enum class PcapNgLinkType(
    val value: UShort,
) {
    NULL(0U),
    ETHERNET(1U),
    AX25(3U),
    IEEE802_5(6U),
    ARCNET_BSD(7U),
    SLIP(8U),
    PPP(9U),
    FDDI(10U),
    PPP_HDLC(50U),
    PPP_ETHER(51U),
    ATM_RFC1483(100U),
    RAW(101U),
    C_HDLC(104U),
    IEEE802_11(105U),
    FRELAY(107U),
    LOOP(108U),
    LINUX_SLL(113U),
    LTALK(114U),
    PFLOG(117U),
    IEEE802_11_PRISM(119U),
    IP_OVER_FC(122U),
    SUNATM(123U),
    IEEE802_11_RADIOTAP(127U),
    ARCNET_LINUX(129U),
    APPLE_IP_OVER_IEEE1394(138U),
    MTP2_WITH_PHDR(139U),
    MTP2(140U),
    MTP3(141U),
    SCCP(142U),
    DOCSIS(143U),
    LINUX_IRDA(144U),
    ;

    companion object {
        fun fromValue(value: UShort) = entries.first { it.value == value }
    }
}
