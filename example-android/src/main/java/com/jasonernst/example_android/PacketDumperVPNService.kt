package com.jasonernst.example_android

import android.content.Intent
import android.net.VpnService

class PacketDumperVPNService: VpnService() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }
}