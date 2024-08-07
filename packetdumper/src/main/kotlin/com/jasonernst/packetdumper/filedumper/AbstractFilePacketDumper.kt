package com.jasonernst.packetdumper.filedumper

import com.jasonernst.packetdumper.AbstractPacketDumper

abstract class AbstractFilePacketDumper : AbstractPacketDumper() {
    abstract var filename: String

    abstract fun open()

    abstract fun close()
}
