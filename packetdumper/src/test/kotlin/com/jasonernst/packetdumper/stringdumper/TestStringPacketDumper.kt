package com.jasonernst.packetdumper.stringdumper

import com.jasonernst.packetdumper.EtherType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class TestStringPacketDumper {
    private val stringPacketDumper = StringPacketDumper()

    @Test fun lessThanSingleLine() {
        val buffer = ByteBuffer.wrap(byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04.toByte()))
        val hexString = stringPacketDumper.dumpBufferToString(buffer, 0, buffer.limit(), false)
        assertEquals("00 01 02 03 04", hexString)

        // ensure that the buffer position is not changed
        assertEquals(0, buffer.position())
    }

    @Test fun oneFullLine() {
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
        val hexString = stringPacketDumper.dumpBufferToString(buffer, 0, buffer.limit(), false)
        assertEquals("00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F\n", hexString)

        // ensure that the buffer position is not changed
        assertEquals(0, buffer.position())
    }

    @Test fun lineAndAHalf() {
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
                    0x10,
                    0x11,
                    0x12,
                    0x13,
                    0x14,
                ),
            )
        val hexString = stringPacketDumper.dumpBufferToString(buffer, 0, buffer.limit(), false)
        assertEquals(
            "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F\n10 11 12 13 14",
            hexString,
        )

        // ensure that the buffer position is not changed
        assertEquals(0, buffer.position())
    }

    @Test fun multipleFullLines() {
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
                    0x10,
                    0x11,
                    0x12,
                    0x13,
                    0x14,
                    0x15,
                    0x16,
                    0x17,
                    0x18,
                    0x19,
                    0x1A,
                    0x1B,
                    0x1C,
                    0x1D,
                    0x1E,
                    0x1F,
                ),
            )
        val hexString = stringPacketDumper.dumpBufferToString(buffer, 0, buffer.limit(), false)
        assertEquals(
            "00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F\n" +
                "10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F\n",
            hexString,
        )

        // ensure that the buffer position is not changed
        assertEquals(0, buffer.position())
    }

    @Test fun lessThanSingleLineFromOffset() {
        val buffer = ByteBuffer.wrap(byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04.toByte()))
        val hexString =
            stringPacketDumper.dumpBufferToString(
                buffer,
                1,
                buffer.limit() - 1,
                false,
            )
        assertEquals("01 02 03 04", hexString)

        // ensure that the buffer position is not changed
        assertEquals(0, buffer.position())
    }

    @Test fun oneFullLineFromOffset() {
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
        val hexString =
            stringPacketDumper.dumpBufferToString(
                buffer,
                1,
                buffer.limit() - 1,
                false,
            )
        assertEquals("01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F", hexString)

        // ensure that the buffer position is not changed
        assertEquals(0, buffer.position())
    }

    @Test fun lineAndAHalfFromOffset() {
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
                    0x10,
                    0x11,
                    0x12,
                    0x13,
                    0x14,
                ),
            )
        val hexString =
            stringPacketDumper.dumpBufferToString(
                buffer,
                1,
                buffer.limit() - 1,
                false,
            )
        assertEquals(
            "01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 10\n11 12 13 14",
            hexString,
        )

        // ensure that the buffer position is not changed
        assertEquals(0, buffer.position())
    }

    @Test fun multipleFullLinesFromOffset() {
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
                    0x10,
                    0x11,
                    0x12,
                    0x13,
                    0x14,
                    0x15,
                    0x16,
                    0x17,
                    0x18,
                    0x19,
                    0x1A,
                    0x1B,
                    0x1C,
                    0x1D,
                    0x1E,
                    0x1F,
                    0x20,
                    0x21,
                    0x22,
                ),
            )
        val hexString =
            stringPacketDumper.dumpBufferToString(
                buffer,
                2,
                buffer.limit() - 2,
                false,
            )
        assertEquals(
            "02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 10 11\n" +
                "12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F 20 21\n22",
            hexString,
        )
    }

    @Test fun lessThanSingleLineWithAddress() {
        val buffer = ByteBuffer.wrap(byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04.toByte()))
        val hexString = stringPacketDumper.dumpBufferToString(buffer, 0, buffer.limit(), true)
        assertEquals("00000000  00 01 02 03 04", hexString)

        // ensure that the buffer position is not changed
        assertEquals(0, buffer.position())
    }

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

    @Test fun lineAndaHalfWithAddress() {
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
                    0x10,
                    0x11,
                    0x12,
                    0x13,
                    0x14,
                ),
            )
        val hexString = stringPacketDumper.dumpBufferToString(buffer, 0, buffer.limit(), true)
        assertEquals(
            "00000000  00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F\n" +
                "00000010  10 11 12 13 14",
            hexString,
        )

        // ensure that the buffer position is not changed
        assertEquals(0, buffer.position())
    }

    @Test fun multipleFullLinesWithAddress() {
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
                    0x10,
                    0x11,
                    0x12,
                    0x13,
                    0x14,
                    0x15,
                    0x16,
                    0x17,
                    0x18,
                    0x19,
                    0x1A,
                    0x1B,
                    0x1C,
                    0x1D,
                    0x1E,
                    0x1F,
                    0x20,
                    0x21,
                    0x22,
                ),
            )
        val hexString = stringPacketDumper.dumpBufferToString(buffer, 0, buffer.limit(), true)
        assertEquals(
            "00000000  00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F\n" +
                "00000010  10 11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F\n00000020  20 21 22",
            hexString,
        )
    }

    @Test fun lessThanFullLineWithAddressAndOffset() {
        val buffer = ByteBuffer.wrap(byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04.toByte()))
        val hexString =
            stringPacketDumper.dumpBufferToString(
                buffer,
                1,
                buffer.limit() - 1,
                true,
            )
        assertEquals("00000000  01 02 03 04", hexString)

        // ensure that the buffer position is not changed
        assertEquals(0, buffer.position())
    }

    @Test fun oneFullLineWithAddressAndOffset() {
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
                    0x10,
                ),
            )
        val hexString =
            stringPacketDumper.dumpBufferToString(
                buffer,
                1,
                buffer.limit() - 1,
                true,
            )
        assertEquals(
            "00000000  01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 10\n",
            hexString,
        )

        // ensure that the buffer position is not changed
        assertEquals(0, buffer.position())
    }

    @Test fun lineAndaHalfWithAddressAndOffset() {
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
                    0x10,
                    0x11,
                    0x12,
                    0x13,
                    0x14,
                ),
            )
        val hexString =
            stringPacketDumper.dumpBufferToString(
                buffer,
                1,
                buffer.limit() - 1,
                true,
            )
        assertEquals(
            "00000000  01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 10\n" +
                "00000010  11 12 13 14",
            hexString,
        )

        // ensure that the buffer position is not changed
        assertEquals(0, buffer.position())
    }

    @Test fun multipleFullLinesWithAddressAndOffset() {
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
                    0x10,
                    0x11,
                    0x12,
                    0x13,
                    0x14,
                    0x15,
                    0x16,
                    0x17,
                    0x18,
                    0x19,
                    0x1A,
                    0x1B,
                    0x1C,
                    0x1D,
                    0x1E,
                    0x1F,
                    0x20,
                    0x21,
                    0x22,
                ),
            )
        val hexString =
            stringPacketDumper.dumpBufferToString(
                buffer,
                1,
                buffer.limit() - 1,
                true,
            )
        assertEquals(
            "00000000  01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F 10\n" +
                "00000010  11 12 13 14 15 16 17 18 19 1A 1B 1C 1D 1E 1F 20\n00000020  21 22",
            hexString,
        )
    }

    @Test fun offsetPastEndOfBuffer() {
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
                    0x10,
                ),
            )

        val hexString = stringPacketDumper.dumpBufferToString(buffer, 17, buffer.limit() - 1, true)
        assertEquals(0, hexString.length)

        // ensure that the buffer position is not changed
        assertEquals(0, buffer.position())
    }

    @Test fun lengthPastEndOfBuffer() {
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
                    0x10,
                ),
            )

        val hexString = stringPacketDumper.dumpBufferToString(buffer, 0, buffer.limit() + 1, true)
        assertEquals(
            "00000000  00 01 02 03 04 05 06 07 08 09 0A 0B 0C 0D 0E 0F\n00000010  10",
            hexString,
        )

        // ensure that the buffer position is not changed
        assertEquals(0, buffer.position())
    }

    @Test fun offsetAndLengthPastEndofBuffer() {
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
                    0x10,
                ),
            )

        val hexString = stringPacketDumper.dumpBufferToString(buffer, 5, 14, true)
        assertEquals("00000000  05 06 07 08 09 0A 0B 0C 0D 0E 0F 10", hexString)

        // ensure that the buffer position is not changed
        assertEquals(0, buffer.position())
    }

    @Test fun dummyHeader() {
        val buffer = ByteBuffer.wrap(byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04))
        val hexString = stringPacketDumper.dumpBufferToString(buffer, 0, buffer.limit(), false, EtherType.IPv4)
        assertEquals("14 C0 3E 55 0B 35 74 D0 2B 29 A5 18 08 00 00 01\n02 03 04", hexString)
    }

    @Test fun dummyHeaderWithAddress() {
        val buffer = ByteBuffer.wrap(byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04))
        val hexString = stringPacketDumper.dumpBufferToString(buffer, 0, buffer.limit(), true, EtherType.IPv4)
        assertEquals("00000000  14 C0 3E 55 0B 35 74 D0 2B 29 A5 18 08 00 00 01\n00000010  02 03 04", hexString)
    }
}
