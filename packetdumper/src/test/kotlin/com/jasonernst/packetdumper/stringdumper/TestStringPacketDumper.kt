package com.jasonernst.packetdumper.stringdumper

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class TestStringPacketDumper {
    private val stringPacketDumper = StringPacketDumper()

    @Test
    fun oneFullLineWithAddress() {
        val buffer =
            ByteBuffer.wrap(
                byteArrayOf(
                    0x00,
                    0x01,
                    0x02,
                    0x03,
                    0x04,
                    0x05,
                    0x06,
                    0x07,
                    0x08,
                    0x09,
                    0x0A,
                    0x0B,
                    0x0C,
                    0x0D,
                    0x0E,
                    0x0F,
                ),
            )
        val hexString = stringPacketDumper.dumpBufferToString(buffer, 0, buffer.limit(), addresses = true)
        assertEquals(
            "00000000  00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F\n",
            hexString,
        )

        // ensure that the buffer position is not changed
        assertEquals(0, buffer.position())
    }
}
