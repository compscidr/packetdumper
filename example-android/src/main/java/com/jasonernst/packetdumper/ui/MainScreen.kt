package com.jasonernst.example_android.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.jasonernst.example_android.PacketDumperVpnService
import com.jasonernst.example_android.VpnPermissionHelper.isVPNPermissionMissing
import com.jasonernst.example_android.model.SessionViewModel
import org.slf4j.LoggerFactory

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(sessionViewModel: SessionViewModel, vpnService: PacketDumperVpnService) {
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