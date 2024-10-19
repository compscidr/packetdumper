package com.jasonernst.packetdumper

interface VpnUiService {
    fun startVPN()
    fun stopVPN()
    fun startPcapServer()
    fun stopPcapServer()
}