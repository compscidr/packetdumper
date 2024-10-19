package com.jasonernst.packetdumper.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.jasonernst.packetdumper.PacketDumperVpnService
import com.jasonernst.packetdumper.VpnPermissionHelper.isVPNPermissionMissing
import com.jasonernst.packetdumper.VpnUiService
import com.jasonernst.packetdumper.model.SessionViewModel
import org.slf4j.LoggerFactory

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(sessionViewModel: SessionViewModel, vpnService: VpnUiService) {
    val logger = LoggerFactory.getLogger("MainScreen")
    val context = LocalContext.current

    if (sessionViewModel.isPermissionScreenHidden().not()) {
        if (isVPNPermissionMissing(context = context)) {
            logger.debug("VPN permission missing")
            VPNPermissionScreen(sessionViewModel)
        } else {
            logger.debug("VPN permission granted")
            SessionScreen(sessionViewModel, vpnService)
        }
    } else {
        logger.debug("VPN permission screen hidden")
        SessionScreen(sessionViewModel, vpnService)
    }
}