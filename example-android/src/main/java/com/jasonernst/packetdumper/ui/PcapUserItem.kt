package com.jasonernst.packetdumper.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PcapUserItem(pcapUser: String) {
    Row(modifier = Modifier.padding(top = 14.dp, start = 24.dp)) {
        Text("A wireshark pcap user is connected from: $pcapUser")
    }
}