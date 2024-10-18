package com.jasonernst.example_android.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.provider.Settings.ACTION_WIFI_SETTINGS
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.jasonernst.example_android.PacketDumperVPNService
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/**
 * Specialized helper function specifically for VPN permission because it's a special case.
 * https://developer.android.com/reference/android/net/VpnService#prepare(android.content.Context)
 *
 * The prepare function will return null if the user has previously consented to the VPN operation,
 * otherwise we need to `startActivityForResult` in order to do this.
 *
 */
fun isVPNPermissionMissing(
    context: Context,
    isPreview: Boolean = false,
): Boolean {
    if (isPreview) {
        return false
    }
    return VpnService.prepare(context) != null
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    val logger = LoggerFactory.getLogger("MainScreen")
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val startForResult =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                logger.debug("VPN permission granted")
            } else {
                logger.debug("VPN permission denied")
            }
        }
    val requestVpnAction: () -> Unit = {
        startForResult.launch(VpnService.prepare(context))
    }

    if (isVPNPermissionMissing(context = context)) {
        logger.debug("VPN permission missing")
        Button(onClick = {
            logger.debug("Requesting VPN permissions")
            scope.launch {
                requestVpnAction()
            }
        }) {
            Text(text = "Request VPN Permissions")
        }
    } else {
        logger.debug("VPN permission granted")
    }

    val sessionViewModel = SessionViewModel.getInstance()
    for (session in sessionViewModel.sessionMap.values) {
        SessionItem(session = session)
    }
}