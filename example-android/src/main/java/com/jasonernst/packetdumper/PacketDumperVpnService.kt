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
import com.jasonernst.knet.Packet.Companion.parseStream
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
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min
import kotlin.random.Random

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
    // just use a fake client since we don't care about the source address since we aren't
    // supporting multiple clients for this application
    val randomPort = Random.nextInt(1024, 65535)
    val clientAddress = InetSocketAddress(Inet4Address.getByName("127.0.0.1"), randomPort)

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
            val bytesToRead = min(MAX_RECEIVE_BUFFER_SIZE, stream.remaining())
            val bytesRead = inputStream.read(readBuffer, 0, bytesToRead)
            if (bytesRead == -1) {
                logger.warn("End of OS stream")
                break
            }
            if (bytesRead > 0) {
                stream.put(readBuffer, 0, bytesRead)
                //logger.debug("Read {} bytes from OS. position: {} remaining {}", bytesRead, stream.position(), stream.remaining())
                stream.flip()
                //logger.debug("After flip: position: {} remaining {}", stream.position(), stream.remaining())
                val packets = parseStream(stream)
                for (packet in packets) {
                    packetDumper.dumpBuffer(ByteBuffer.wrap(packet.toByteArray()), etherType = EtherType.DETECT)
                }
                // logger.debug("After parse: position: {} remaining {}", stream.position(), stream.remaining())
                kAnonProxy.handlePackets(packets, clientAddress)
            }
        }
    }

    private fun readFromInternetWriteToOS(outputStream: AutoCloseOutputStream) {
        while (running.get()) {
            val packet = kAnonProxy.takeResponse(clientAddress)
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

//            val protocol = IpType.fromValue(ipHeader!!.protocol)
//            val key = Session.getKey(
//                ipHeader.destinationAddress.toString(),
//                sourcePort.toInt(),
//                ipHeader.sourceAddress.toString(),
//                destinationPort.toInt(),
//                protocol.toString()
//            )
//            val session = sessionViewModel.sessionMap.getOrPut(key) {
//                Session(
//                    ipHeader.sourceAddress.toString(),
//                    sourcePort.toInt(),
//                    ipHeader.destinationAddress.toString(),
//                    destinationPort.toInt(),
//                    protocol.toString(),
//                    System.currentTimeMillis()
//                )
//            }
//            session.incomingPackets.intValue++
//            session.incomingBytes.intValue += ipHeader.getTotalLength().toInt()

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