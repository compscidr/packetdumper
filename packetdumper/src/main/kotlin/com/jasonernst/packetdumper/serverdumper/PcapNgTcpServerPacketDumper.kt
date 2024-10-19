package com.jasonernst.packetdumper.serverdumper

import com.jasonernst.packetdumper.ethernet.EtherType
import com.jasonernst.packetdumper.ethernet.EthernetHeader.Companion.prependDummyHeader
import com.jasonernst.packetdumper.pcapng.PcapNgEnhancedPacketBlock
import com.jasonernst.packetdumper.pcapng.PcapNgInterfaceDescriptionBlock
import com.jasonernst.packetdumper.pcapng.PcapNgSectionHeaderBlockLive
import com.jasonernst.packetdumper.pcapng.PcapNgSimplePacketBlock
import org.slf4j.LoggerFactory
import java.io.BufferedOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.nio.ByteBuffer
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Sets up a server listening on the specified TCP port. (default is DEFAULT_PORT)
 *
 * Wireshark can then connect to it as follows: wireshark -k -i TCP@<ip address>:<port>
 *
 * Dumps packets over the network in the PCAP-NG format.
 *
 * @param listenPort The port to listen on.
 * @param isSimple If true, the file will be written in the simple format.
 *  If false, it will be written in the enhanced format.
 */
class PcapNgTcpServerPacketDumper(
    private val listenPort: Int = DEFAULT_PORT,
    private val isSimple: Boolean = true,
    private val callback: ConnectedUsersChangedCallback? = null,
) : AbstractServerPacketDumper() {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val isRunning = AtomicBoolean(false)

    // TODO: use a more kotlin way to do this with coroutines instead of a thread
    private var socket: ServerSocket? = null
    private var listenerThread: Thread? = null
    private val connectionQueue = Collections.synchronizedList(mutableListOf<Socket>())

    companion object {
        const val DEFAULT_PORT = 19000
    }

    override fun start() {
        if (isRunning.get()) {
            logger.error("Trying to start a server that is already running")
            return
        }
        listenerThread =
            Thread({
                logger.trace("WiresharkTcpDump starting")
                try {
                    socket = ServerSocket(listenPort)
                } catch (e: Exception) {
                    logger.error("Error starting WiresharkTcpDumper: $e")
                    return@Thread
                }
                logger.trace("WiresharkTcpDump listening on {}", socket!!.localSocketAddress)
                logAllIPAddresses()
                isRunning.set(true)

                while (isRunning.get()) {
                    try {
                        val client = socket!!.accept()
                        logger.trace(
                            "WiresharkTcpDump accepted connection from {}",
                            client.remoteSocketAddress,
                        )

                        try {
                            val outputstream = BufferedOutputStream(client.getOutputStream())
                            outputstream.write(PcapNgSectionHeaderBlockLive.toBytes())
                            outputstream.flush()

                            val interfaceblock = PcapNgInterfaceDescriptionBlock().toBytes()

                            outputstream.write(interfaceblock)
                            outputstream.flush()

                            connectionQueue.add(client)
                            issueCallback()
                        } catch (e: Exception) {
                            logger.warn(
                                "Error writing to wireshark client, it may have " +
                                    "disconnected from us before we wrote the pcap header: $e",
                            )
                            continue
                        }
                    } catch (e: Exception) {
                        logger.warn(
                            "WiresharkTcpDump error accepting connection, possibly" +
                                " shutting down: $e",
                        )
                        continue
                    }
                }
            }, "PcapNgTcpServerPacketDumper listener")
        listenerThread!!.start()

        // wait for isRunning to be set to true
        while (!isRunning.get()) {
            Thread.sleep(100)
        }
    }

    override fun stop() {
        if (!isRunning.get()) {
            logger.error("Trying to stop a server that is already stopped")
            return
        }
        isRunning.set(false)
        socket?.close()
        logger.debug("Waiting for listener thread to finish")
        listenerThread?.join()
        logger.debug("Closing all connections")
        for (connection in connectionQueue) {
            try {
                connection.close()
            } catch (e: Exception) {
                logger.error("Error closing connection", e)
            }
        }
    }

    override fun dumpBuffer(
        buffer: ByteBuffer,
        offset: Int,
        length: Int,
        addresses: Boolean,
        etherType: EtherType?,
    ) {
        if (connectionQueue.isEmpty()) {
            // if there are no connected clients, no reason to dump
            return
        }
        val startingPosition = buffer.position()
        val originalLimit = buffer.limit()
        val conversionBuffer =
            if (etherType != null) {
                prependDummyHeader(buffer, offset, length, etherType)
            } else {
                val actualLimit = minOf(originalLimit, offset + length)
                buffer.limit(actualLimit)
            }
        val packetBlock =
            if (isSimple) {
                PcapNgSimplePacketBlock(conversionBuffer.array())
            } else {
                // convert the nano time to microseconds since that is the default if we don't set
                // the if_tsresol in the interface description block
                val timestamp = System.nanoTime() / 1000
                PcapNgEnhancedPacketBlock(conversionBuffer.array(), timestamp = timestamp)
            }
        buffer.position(startingPosition)
        buffer.limit(originalLimit)

        var failedConnection = false
        with(connectionQueue.iterator()) {
            forEach {
                try {
                    val outputstream = BufferedOutputStream(it.getOutputStream())
                    outputstream.write(packetBlock.toBytes())
                    outputstream.flush()
                } catch (e: Exception) {
                    remove()
                    failedConnection = true
                }
            }
        }

        if (failedConnection) {
            issueCallback()
        }
    }

    private fun issueCallback() {
        callback?.onConnectedUsersChanged(connectionQueue.map { it.remoteSocketAddress.toString() })
    }

    private fun logAllIPAddresses(
        excludeInterfaces: List<String> = listOf("bump", "docker", "virbr", "veth", "tailscale", "dummy", "tun"),
    ) {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            if (interfaces == null) {
                logger.error("No network interfaces found")
                return
            }
            for (networkInterface in interfaces) {
                var excluded = false
                for (excludeInterface in excludeInterfaces) {
                    if (networkInterface.displayName.contains(excludeInterface)) {
                        excluded = true
                        break
                    }
                }
                if (excluded) {
                    continue
                }
                if (networkInterface.isUp.not()) {
                    continue
                }
                val addresses = networkInterface.inetAddresses.toList()
                if (addresses.isEmpty()) {
                    continue
                }
                logger.trace("Network Interface: ${networkInterface.name}")
                for (inetAddress in addresses) {
                    logger.trace("  IP Address: ${inetAddress.hostAddress}")
                }
            }
        } catch (e: SocketException) {
            logger.error("Error getting network interfaces", e)
        }
    }
}
