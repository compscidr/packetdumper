package com.jasonernst.packetdumper

import android.content.Context
import android.net.VpnService

object VpnPermissionHelper {
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
}