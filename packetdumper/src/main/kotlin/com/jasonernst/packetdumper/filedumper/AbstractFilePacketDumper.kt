package com.jasonernst.packetdumper.filedumper

import com.jasonernst.packetdumper.AbstractPacketDumper
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The base class for all file packet dumpers. We leave it as an abstract class so that we don't
 * have to implement the dumpBuffer method, and it can be implemented by the specific file dumper.
 */
abstract class AbstractFilePacketDumper(
    path: String,
    name: String,
    ext: String,
) : AbstractPacketDumper() {
    private val logger = LoggerFactory.getLogger(javaClass)

    // keep the filename public to make testing easier
    val filename: String = "$path/${name}_${LocalDateTime.now()}.$ext"
    protected val isOpen = AtomicBoolean(false)
    protected lateinit var file: File
    protected var loggedError = false

    open fun open() {
        if (isOpen.get()) {
            logger.error("Trying to open a file that is already open")
            return
        }
        file = File(filename)
        logger.debug("Opened file $filename")
        isOpen.set(true)
    }

    open fun close() {
        if (!isOpen.get()) {
            logger.error("Trying to close a file that is already closed")
            return
        }
        isOpen.set(false)
    }
}
