package com.jasonernst.packetdumper.serverdumper

import com.jasonernst.packetdumper.AbstractPacketDumper

abstract class AbstractServerPacketDumper : AbstractPacketDumper() {
    abstract fun start()

    abstract fun stop()
}
