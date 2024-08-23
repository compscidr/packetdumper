package com.jasonernst.packetdumper.filedumper

import com.jasonernst.packetdumper.ethernet.EtherType
import com.jasonernst.packetdumper.stringdumper.StringPacketDumper
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.ByteBuffer

/**
 * Dumps packets to a text file in hexdump format.
 *
 * If addresses and EtherType is set, the output will be compatible with Wireshark Hexdump. It
 * can be imported with `File -> Import from Hexdump.`
 */
class TextFilePacketDumper(
    path: String,
    name: String,
) : AbstractFilePacketDumper(path, name, "txt") {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val stringDumper = StringPacketDumper()

    /**
     * Dumps buffers into hexdump text files that can be imported by wireshark. The format is as follows:
     *
     * offset(hex)  up to 16 bytes of data per line, separated by a space, ending in a newline
     * offset(hex)  up to 16 bytes of data per line, separated by a space, ending in a newline
     *
     * offset(hex)  up to 16 bytes of data per line, separated by a space, ending in a newline
     * offset(hex)  up to 16 bytes of data per line, separated by a space, ending in a newline
     *
     * Each block of offsets is separated by a blank newline, and represents a new packet / dump. When
     * the new dump starts, the offset can restart at 0.
     *
     */
    override fun dumpBuffer(
        buffer: ByteBuffer,
        offset: Int,
        length: Int,
        addresses: Boolean,
        etherType: EtherType?,
    ) {
        if (!isOpen.get()) {
            if (!loggedError) {
                logger.error("Trying to dump to a file that is not open")
                loggedError = true
            }
            return
        }
        // extra space added so that the hexdump import doesn't skip the last byte, see:
        // https://osqa-ask.wireshark.org/questions/39177/wireshark-import-hex-dump-always-strip-last-byte-of-the-packet/
        val output = stringDumper.dumpBufferToString(buffer, offset, length, addresses, etherType) + " "
        file.writeText(output)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)

        /**
         * Attempts to parse a text file into a ByteBuffer
         */
        fun parseFile(
            filename: String,
            addresses: Boolean = false,
            etherType: EtherType? = null,
        ): ByteBuffer {
            val stringDumper = StringPacketDumper()
            val file = File(filename)
            var text = file.readText()
            logger.debug("raw text: $text")

            if (addresses) {
                val lines = text.split("\n")
                text = ""
                for (line in lines) {
                    val firstSpace = line.indexOfFirst { it == ' ' }
                    text += line.drop(firstSpace + 1).trim() + " "
                }
                text = text.trimEnd() // remove the trailing space
                logger.debug("No address text: $text")
            }

            if (etherType != null) {
                // note: we multiple by 3, because each byte is represented by two hex characters and a
                // space character
                text = text.drop(42)
                logger.debug("No ether type text: $text")
            }

            // remove any remaining newlines
            text = text.replace("\n", " ")

            // each space separated value is a hex value, so we need to turn it back into bytes
            val readBuffer = ByteBuffer.wrap(text.split(" ").map { it.toInt(16).toByte() }.toByteArray())

            logger.debug("read buffer: ${stringDumper.dumpBufferToString(readBuffer, 0, readBuffer.limit())}")
            return readBuffer
        }
    }
}
