package com.jasonernst.example_android

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import android.os.ParcelFileDescriptor.AutoCloseOutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicBoolean

class PacketDumperVPNService: VpnService() {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var vpnFileDescriptor: ParcelFileDescriptor? = null
    private val readJob = SupervisorJob() // https://stackoverflow.com/a/63407811
    private val writeJob = SupervisorJob()
    private val readScope = CoroutineScope(Dispatchers.IO + readJob)
    private val writeScope = CoroutineScope(Dispatchers.IO + writeJob)
    private val running = AtomicBoolean(false)
    private val readBuffer = ByteArray(MAX_RECEIVE_BUFFER_SIZE)
    private val packetQueue = LinkedBlockingDeque<Packet>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.debug("onStartCommand: {} {} {}", intent, flags, startId)
        prepareVPN()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun prepareVPN() {
        if (running.get()) {
            logger.error("VPN already running")
            return
        }

        val builder = Builder()
            .addAddress(VPN_ADDRESS, VPN_SUBNET_MASK)
            .addAddress(VPN6_ADDRESS, VPN_SUBNET6_MASK)
            .addDnsServer(DNS_SERVER)
            .addDnsServer(DNS6_SERVER)
            .addRoute("0.0.0.0", 0)
            .addRoute("2000::", 3) // https://wiki.strongswan.org/issues/1261

        vpnFileDescriptor = builder.establish()
        logger.debug("VPN established: {}", vpnFileDescriptor)
        val inputStream = AutoCloseInputStream(vpnFileDescriptor)
        val outputStream = AutoCloseOutputStream(vpnFileDescriptor)
        running.set(true)

        readScope.launch {
            readFromOSWriteToInternet(inputStream)
        }

        writeScope.launch {
            readFromInternetWritetoOS(outputStream)
        }
    }

    private suspend fun readFromOSWriteToInternet(inputStream: AutoCloseInputStream) {
        withContext(Dispatchers.IO) {
            val stream = ByteBuffer.allocate(MAX_STREAM_BUFFER_SIZE)
            while (running.get()) {
                val bytesRead: Int
                try {
                    bytesRead = inputStream.read(readBuffer)
                    if (bytesRead == -1) {
                        logger.debug("End of stream")
                        break
                    }
                    if (bytesRead == 0) {
                        continue
                    }
                    logger.debug("Read {} bytes from OS", bytesRead)
                } catch (e: Exception) {
                    logger.error("Error reading from OS", e)
                    break
                }

                stream.put(readBuffer, 0, bytesRead)
                stream.flip()
                val packets = parseStream(stream)


            }
        }
    }

    /**
     * The packets will be either IPv4 or Ipv6 headers, followed by NextHeader(s) which are typically
     * TCP, UDP, ICMP, etc. This is followed by the optional payload.
     *
     * For TCP packets, we need to make a request on behalf of the client on a protected TCP socket.
     * We then need to listen to the return traffic and send it back to the client, and ensure that
     * the sequence numbers are maintained etc.
     *
     * For UDP packets, we can just send them to the internet and listen for the return traffic and
     * then just send it back to the client.
     *
     * For ICMP, we need to use an ICMP socket (https://github.com/compscidr/icmp) to send the
     * request and listen for the return traffic. We then return the ICMP result to the client. This
     * may be unreachable, time exceeded, etc, or just a successful ping response.
     */
    private fun parseStream(stream: ByteBuffer): List<Packet> {
        val packets = mutableListOf<Packet>()
        while (stream.hasRemaining()) {
            val position = stream.position()
            try {
                val packet = Packet.fromStream(stream)
                packets.add(packet)
            } catch (e: IllegalArgumentException) {
                // don't bother to rewind the stream, just log and continue at position + 1
                logger.error("Error parsing stream", e)
                stream.position(position + 1)
            } catch (e: PacketTooShortException) {
                // rewind the stream to before we tried parsing so we can try again later
                stream.position(position)
                break
            }
        }
        return packets

    }

    private suspend fun readFromInternetWritetoOS(outputStream: AutoCloseOutputStream) {
        withContext(Dispatchers.IO) {
            while (running.get()) {
                packetQueue.take().let { packet ->
                    val bytesToWrite = packet.toByteArray()
                    outputStream.write(bytesToWrite)
                    outputStream.flush()
                    logger.debug("Wrote {} bytes to OS", bytesToWrite.size)
                }
            }
        }
    }

    override fun stopService(name: Intent?): Boolean {
        vpnFileDescriptor?.close()
        readJob.cancel()
        writeJob.cancel()
        return super.stopService(name)
    }

    companion object {
        private const val VPN_ADDRESS = "10.10.19.2"
        private const val VPN_SUBNET_MASK = 24
        private const val VPN6_ADDRESS = "fd00:10:10:19::2"
        private const val VPN_SUBNET6_MASK = 64
        private const val DNS_SERVER = "8.8.8.8"
        private const val DNS6_SERVER = "2001:4860:4860::8888"
        private const val MAX_STREAM_BUFFER_SIZE = 1048576 // max we can write into the stream without parsing
        private const val MAX_RECEIVE_BUFFER_SIZE = 1024 // max amount we can recv in one read
    }
}