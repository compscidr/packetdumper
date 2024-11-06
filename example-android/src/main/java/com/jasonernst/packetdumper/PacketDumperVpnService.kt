package com.jasonernst.packetdumper

import android.content.Intent
import android.net.VpnService
import android.os.Binder
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import android.os.ParcelFileDescriptor.AutoCloseOutputStream
import androidx.preference.PreferenceManager
import com.jasonernst.packetdumper.model.SessionViewModel
import com.jasonernst.icmp.android.IcmpAndroid
import com.jasonernst.kanonproxy.KAnonProxy
import com.jasonernst.kanonproxy.VpnProtector
import com.jasonernst.knet.Packet
import com.jasonernst.knet.network.ip.IpType
import com.jasonernst.knet.transport.TransportHeader
import com.jasonernst.knet.transport.tcp.TcpHeader
import com.jasonernst.packetdumper.ethernet.EtherType
import com.jasonernst.packetdumper.serverdumper.ConnectedUsersChangedCallback
import com.jasonernst.packetdumper.serverdumper.PcapNgTcpServerPacketDumper
import com.jasonernst.packetdumper.stringdumper.StringPacketDumper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

class PacketDumperVpnService: VpnService(), VpnProtector, VpnUiService, ConnectedUsersChangedCallback {
    private val logger = LoggerFactory.getLogger(javaClass)
    private var vpnFileDescriptor: ParcelFileDescriptor? = null
    private val readJob = SupervisorJob() // https://stackoverflow.com/a/63407811
    private val writeJob = SupervisorJob()
    private val readScope = CoroutineScope(Dispatchers.IO + readJob)
    private val writeScope = CoroutineScope(Dispatchers.IO + writeJob)
    private val running = AtomicBoolean(false)
    private val readBuffer = ByteArray(MAX_RECEIVE_BUFFER_SIZE)
    private val kAnonProxy = KAnonProxy(IcmpAndroid, this)
    private lateinit var sessionViewModel: SessionViewModel
    private val packetDumper = PcapNgTcpServerPacketDumper(callback = this, isSimple = false)
    private val binder = LocalBinder()

    /**
     * Class used for the client Binder. Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods.
        fun getService(): PacketDumperVpnService = this@PacketDumperVpnService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.debug("onStartCommand: {} {} {}", intent, flags, startId)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        logger.debug("ON CREATE CALLED")
        sessionViewModel = SessionViewModel.getInstance(PreferenceManager.getDefaultSharedPreferences(applicationContext))
    }

    override fun startVPN() {
        if (running.get()) {
            logger.error("VPN already running")
            return
        }

        kAnonProxy.start()

        val builder = Builder()
            .addAddress(VPN_ADDRESS, VPN_SUBNET_MASK)
            //.addAddress(VPN6_ADDRESS, VPN_SUBNET6_MASK)
            .addDnsServer(DNS_SERVER)
            //.addDnsServer(DNS6_SERVER)
            .setMtu(MAX_RECEIVE_BUFFER_SIZE)
            .addRoute("0.0.0.0", 0)
        //.addRoute("2000::", 3) // https://wiki.strongswan.org/issues/1261

        vpnFileDescriptor = builder.establish()
        logger.debug("VPN established, FD: {}", vpnFileDescriptor?.fd)
        val inputStream = AutoCloseInputStream(vpnFileDescriptor)
        val outputStream = AutoCloseOutputStream(vpnFileDescriptor)
        running.set(true)
        sessionViewModel.serviceStarted()

        readScope.launch {
            Thread.currentThread().name = "OS Reader"
            readFromOSWriteToInternet(inputStream)
        }

        writeScope.launch {
            Thread.currentThread().name = "OS Writer"
            readFromInternetWriteToOS(outputStream)
        }
    }

    override fun stopVPN() {
        running.set(false)
        packetDumper.stop()
        vpnFileDescriptor?.close()
        readJob.cancel()
        writeJob.cancel()
        kAnonProxy.stop()
        sessionViewModel.serviceStopped()
    }

    override fun startPcapServer() {
        packetDumper.start()
        sessionViewModel.pcapServerStarted()
    }

    override fun onConnectedUsersChanged(connectedUsers: List<String>) {
        logger.debug("Connected users changed: {}", connectedUsers)
        sessionViewModel.pcapUsersChanged(connectedUsers)
    }

    override fun stopPcapServer() {
        packetDumper.stop()
        sessionViewModel.pcapServerStopped()
    }

    private fun readFromOSWriteToInternet(inputStream: AutoCloseInputStream) {
        val stream = ByteBuffer.allocate(MAX_STREAM_BUFFER_SIZE)
        while (running.get()) {
            // fill up the buffer with data from the OS over multiple reads, or until
            // there is no more data to read
            var totalBytesRead = 0
            do {
                // make sure we don't overlfow the buffer
                var bytesToRead = min(MAX_RECEIVE_BUFFER_SIZE, stream.remaining())
                val bytesRead: Int = inputStream.read(readBuffer, 0, bytesToRead)
                if (bytesRead == -1) {
                    logger.warn("End of OS stream")
                    break
                }
                if (bytesRead > 0) {
                    //logger.debug("About to write {} bytes to buffer at position: {}", bytesRead, stream.position())
                    stream.put(readBuffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                }
            } while (bytesRead > 0 && stream.hasRemaining())
            if (totalBytesRead > 0) {
                // logger.debug("Read {} bytes from OS", totalBytesRead)
                stream.flip()
                val packets = parseStream(stream)

//                val packetsToHandle = mutableListOf<Packet>()
//                for (packet in packets) {
//                    if (packet.nextHeaders is TcpHeader) {
//                        if (packet.ipHeader!!.destinationAddress == Inet4Address.getByName("138.68.242.6")) {
//                            packetsToHandle.add(packet)
//                            packetDumper.dumpBuffer(ByteBuffer.wrap(packet.toByteArray()), etherType = EtherType.DETECT)
//                        }
//                    } else {
//                        packetsToHandle.add(packet)
//                        packetDumper.dumpBuffer(ByteBuffer.wrap(packet.toByteArray()), etherType = EtherType.DETECT)
//                    }
//                }
//                logger.debug("Parsed {} packets from OS, sending to proxy", packetsToHandle.size)
//                kAnonProxy.handlePackets(packetsToHandle)'
                kAnonProxy.handlePackets(packets)
            } else {
                // Thread.sleep(100) // wait for data to arrive
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
        //logger.debug("GOT STREAM: \n{}", StringPacketDumper().dumpBufferToString(buffer = stream, addresses = true, etherType = null))
        val packets = mutableListOf<Packet>()
        while (stream.hasRemaining()) {
            val position = stream.position()
            try {
                val packet = Packet.fromStream(stream)
                if (packet.ipHeader == null || packet.nextHeaders == null || packet.payload == null) {
                    logger.warn("Packet is missing headers or payload, skipping")
                    continue
                }
                //logger.debug("Parsed packet: {}", packet)
                //logger.debug("Stream position after parsing: {} limit: {}", stream.position(), stream.limit())
                val ipHeader = packet.ipHeader
                val nextHeader = packet.nextHeaders

                val sourcePort = if (nextHeader is TransportHeader) {
                    nextHeader.sourcePort
                } else {
                    0u
                }
                val destinationPort = if (nextHeader is TransportHeader) {
                    nextHeader.destinationPort
                } else {
                    0u
                }

                val protocol = IpType.fromValue(ipHeader!!.protocol)
                val key = Session.getKey(
                    ipHeader.sourceAddress.toString(),
                    sourcePort.toInt(),
                    ipHeader.destinationAddress.toString(),
                    destinationPort.toInt(),
                    protocol.toString()
                )
                val session = sessionViewModel.sessionMap.getOrPut(key) {
                    Session(
                        ipHeader.sourceAddress.toString(),
                        sourcePort.toInt(),
                        ipHeader.destinationAddress.toString(),
                        destinationPort.toInt(),
                        protocol.toString(),
                        System.currentTimeMillis()
                    )
                }
                session.outgoingPackets.intValue++
                session.outgoingBytes.intValue += ipHeader.getTotalLength().toInt()
                packets.add(packet)
            } catch (e: IllegalArgumentException) {
                // don't bother to rewind the stream, just log and continue at position + 1
                logger.error("Error parsing stream: ", e)
                stream.position(position + 1)
            } catch (e: com.jasonernst.knet.PacketTooShortException) {
                logger.warn("Packet too short to parse, trying again when more data arrives: {}", e.message)
                //logger.debug("POSITION: {} LIMIT: {}, RESETTING TO START: {}", stream.position(), stream.limit(), position)
                // rewind the stream to before we tried parsing so we can try again later
                stream.position(position)
                break
            }
        }
        //logger.debug("Stream position before compact: {} limit: {}", stream.position(), stream.limit())
        stream.compact()
        //logger.debug("Stream position after compact: {} limit: {}", stream.position(), stream.limit())
        return packets
    }

    private fun readFromInternetWriteToOS(outputStream: AutoCloseOutputStream) {
        while (running.get()) {
            val packet = kAnonProxy.takeResponse()
            logger.debug("Got packet from proxy: {}", packet.nextHeaders)
            if (packet.ipHeader == null || packet.nextHeaders == null || packet.payload == null) {
                logger.warn("Packet is missing headers or payload, skipping")
                continue
            }
            packetDumper.dumpBuffer(ByteBuffer.wrap(packet.toByteArray()), etherType = EtherType.DETECT)
            val bytesToWrite = packet.toByteArray()

            val ipHeader = packet.ipHeader
            val nextHeader = packet.nextHeaders

            // source / dest are swapped for the return traffic so we get the same "session"
            val sourcePort = if (nextHeader is TransportHeader) {
                nextHeader.destinationPort
            } else {
                0u
            }
            val destinationPort = if (nextHeader is TransportHeader) {
                nextHeader.sourcePort
            } else {
                0u
            }

            val protocol = IpType.fromValue(ipHeader!!.protocol)
            val key = Session.getKey(
                ipHeader.destinationAddress.toString(),
                sourcePort.toInt(),
                ipHeader.sourceAddress.toString(),
                destinationPort.toInt(),
                protocol.toString()
            )
            val session = sessionViewModel.sessionMap.getOrPut(key) {
                Session(
                    ipHeader.sourceAddress.toString(),
                    sourcePort.toInt(),
                    ipHeader.destinationAddress.toString(),
                    destinationPort.toInt(),
                    protocol.toString(),
                    System.currentTimeMillis()
                )
            }
            session.incomingPackets.intValue++
            session.incomingBytes.intValue += ipHeader.getTotalLength().toInt()

            try {
                outputStream.write(bytesToWrite)
                outputStream.flush()
                logger.debug("Wrote {} bytes to OS", bytesToWrite.size)
            } catch (e: Exception) {
                logger.error("Error writing to OS, probably shutting down ", e)
            }
        }
        logger.warn("OS Writer thread exiting")
    }

    override fun onBind(intent: Intent): IBinder? {
        logger.debug("ON BIND CALLED")
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        logger.debug("ON UNBIND CALLED")
        // All clients have unbound with unbindService()
        return super.onUnbind(intent)
    }

    override fun onRevoke() {
        logger.warn("onRevoke!!")
        super.onRevoke()
    }

    override fun onDestroy() {
        logger.debug("ON DESTROY CALLED")
        stopVPN()
        stopPcapServer()
        logger.debug("ON DESTROY COMPLETED")
        super.onDestroy()
    }

    override fun protectSocketFd(socket: Int) {
        protect(socket)
    }

    override fun protectUDPSocket(socket: DatagramSocket) {
        protect(socket)
    }

    override fun protectTCPSocket(socket: Socket) {
        protect(socket)
    }

    companion object {
        private const val VPN_ADDRESS = "10.10.19.2"
        private const val VPN_SUBNET_MASK = 24
        private const val VPN6_ADDRESS = "fd00:10:10:19::2"
        private const val VPN_SUBNET6_MASK = 64
        private const val DNS_SERVER = "8.8.8.8"
        private const val DNS6_SERVER = "2001:4860:4860::8888"
        private const val MAX_STREAM_BUFFER_SIZE = 1048576 // max we can write into the stream without parsing
        private const val MAX_RECEIVE_BUFFER_SIZE = 1500   // max amount we can recv in one read (should be the MTU or bigger probably)
    }
}