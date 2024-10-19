package com.jasonernst.packetdumper.ui

import com.jasonernst.packetdumper.VpnUiService

/**
 * Just so previews work correctly
 */
object DummyVpnUiService: VpnUiService {
    override fun startVPN() {
        // no-op
    }

    override fun stopVPN() {
        // no-op
    }

    override fun startPcapServer() {
        // no-op
    }

    override fun stopPcapServer() {
        // no-op
    }

}