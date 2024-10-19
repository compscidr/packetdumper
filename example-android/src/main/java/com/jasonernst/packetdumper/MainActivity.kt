package com.jasonernst.example_android

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.preference.PreferenceManager
import com.jasonernst.example_android.model.SessionViewModel
import com.jasonernst.example_android.ui.MainScreen
import org.slf4j.LoggerFactory

class MainActivity: ComponentActivity() {
    private val logger = LoggerFactory.getLogger(javaClass)
    // example of this pattern: https://github.com/JustAmalll/Stopwatch/blob/master/app/src/main/java/dev/amal/stopwatch/MainActivity.kt
    private lateinit var vpnService: PacketDumperVpnService
    private var isBound by mutableStateOf(false)
    val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            logger.debug("Bound to VPN service")
            // We've bound to LocalService, cast the IBinder and get LocalService instance.
            val binder = service as PacketDumperVpnService.LocalBinder
            vpnService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    /**
     * If we try to use an unbound service, as soon as we call establish on the VPN service we are
     * no longer able to have it stop with stopService. I have a feeling it's because under the hood
     * the VPN service is actually a bound service once we call establish, so we need to bind to it
     * in order to stop it correctly.
     */
    override fun onStart() {
        logger.debug("activity onStart")
        super.onStart()
        Intent(this, PacketDumperVpnService::class.java).also { intent ->
            bindService(intent, connection, BIND_AUTO_CREATE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        logger.debug("activity onCreate")
        super.onCreate(savedInstanceState)
        val sessionViewModel = SessionViewModel.getInstance(PreferenceManager.getDefaultSharedPreferences(applicationContext))

        setContent {
            MaterialTheme {
                if (isBound) {
                    MainScreen(sessionViewModel, vpnService)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // TODO: stop the VPN service
    }
}