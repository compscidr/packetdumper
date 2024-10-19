package com.jasonernst.packetdumper.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.jasonernst.knet.network.ip.IpType
import com.jasonernst.packetdumper.Session
import com.jasonernst.packetdumper.VpnPermissionHelper
import com.jasonernst.packetdumper.VpnUiService
import com.jasonernst.packetdumper.model.SessionViewModel
import org.slf4j.LoggerFactory

@Composable
fun SessionScreen(sessionViewModel: SessionViewModel, vpnService: VpnUiService) {
    val context = LocalContext.current
    val sortedSessions = sessionViewModel.sessionMap.values.sortedBy { it.timeStarted }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(top = 14.dp, start = 24.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                Text("Packet Dumper Service")
            }
            Column(modifier = Modifier.fillMaxWidth().padding(end = 24.dp), horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Switch(
                    checked = sessionViewModel.isServiceStarted(),
                    onCheckedChange = {
                        if (it) {
                            if (VpnPermissionHelper.isVPNPermissionMissing(context)) {
                                sessionViewModel.showPermissionScreen()
                            } else {
                                vpnService.startVPN()
                            }
                        } else {
                            val logger = LoggerFactory.getLogger("SessionScreen")
                            logger.debug("Stopping service")
                            vpnService.stopVPN()
                        }
                    }
                )
            }
        }
        Row {
            Column(modifier = Modifier.padding(top = 14.dp, start = 24.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                Text("Wireshark Pcapng server")
            }
            Column(modifier = Modifier.fillMaxWidth().padding(end = 24.dp), horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Switch(
                    checked = sessionViewModel.isPcapServerStarted(),
                    onCheckedChange = {
                        if (it) {
                            vpnService.startPcapServer()
                        } else {
                            vpnService.stopPcapServer()
                        }
                    }
                )
            }
        }

        LazyColumn {
            items(sessionViewModel.getPcapUsers()) { pcapUser ->
                PcapUserItem(pcapUser = pcapUser)
            }
        }

        LazyColumn(modifier = Modifier.padding(start = 24.dp, top = 14.dp, end = 24.dp)) {
            items(sortedSessions) { session ->
                SessionItem(session = session)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSessionScreen() {
    val context = LocalContext.current
    val sessionViewModel = SessionViewModel.getInstance(PreferenceManager.getDefaultSharedPreferences(context))
    sessionViewModel.pcapUsersChanged(listOf("127.0.0.1"))
    sessionViewModel.sessionMap.put("1", Session("127.0.0.1", 1234, "8.8.8.8", 443, IpType.TCP.name, System.currentTimeMillis() - 1000))
    SessionScreen(sessionViewModel, DummyVpnUiService)
}