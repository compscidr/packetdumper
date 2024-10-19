package com.jasonernst.packetdumper.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.jasonernst.packetdumper.PacketDumperVpnService
import com.jasonernst.packetdumper.VpnPermissionHelper
import com.jasonernst.packetdumper.model.SessionViewModel
import org.slf4j.LoggerFactory

@Composable
fun SessionScreen(sessionViewModel: SessionViewModel, vpnService: PacketDumperVpnService) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(top = 12.dp, start = 24.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
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

        for (session in sessionViewModel.sessionMap.values) {
            SessionItem(session = session)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSessionScreen() {
    val context = LocalContext.current
    val sessionViewModel = SessionViewModel.getInstance(PreferenceManager.getDefaultSharedPreferences(context))
    val vpnService = PacketDumperVpnService()
    SessionScreen(sessionViewModel, vpnService)
}