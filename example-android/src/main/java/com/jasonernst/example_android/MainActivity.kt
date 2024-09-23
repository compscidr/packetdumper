package com.jasonernst.example_android

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.jasonernst.example_android.ui.MainScreen

class MainActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen()
            }
        }

        if (VpnService.prepare(applicationContext) == null) {
            val intent = Intent(applicationContext, PacketDumperVPNService::class.java)
            startService(intent)
        }
    }
}