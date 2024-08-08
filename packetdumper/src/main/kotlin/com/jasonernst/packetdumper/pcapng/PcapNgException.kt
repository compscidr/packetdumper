package com.jasonernst.packetdumper.pcapng

import java.lang.Exception

class PcapNgException : Exception {
    constructor(message: String?) : super(message) {}
    constructor(message: String?, throwable: Throwable?) : super(message, throwable) {}

    companion object {
        private const val serialVersionUID = 1L
    }
}
