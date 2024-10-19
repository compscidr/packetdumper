package com.jasonernst.packetdumper.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.jasonernst.packetdumper.Session
import kotlinx.coroutines.delay

@Composable
fun ExpandedSessionItem(session: Session) {
    Column {
        Row {
            Text("Source: ")
            Text(text = session.sourceAddress)
            Text(":")
            Text(text = session.sourcePort.toString())
        }
        Row {
            Text("Destination: ")
            Text(text = session.destinationAddress)
            Text(":")
            Text(text = session.destinationPort.toString())
        }
        Row {
            // todo: convert the protocol to the enum type name
            Text(text = session.protocol.toString())
        }
        // https://stackoverflow.com/a/74819210
        var timeSinceStartedInSeconds = remember { mutableLongStateOf((System.currentTimeMillis() - session.timeStarted) / 1000) }
        var isRunning = remember { mutableStateOf(false) }
        LifecycleResumeEffect(Unit) {
            isRunning.value = true
            onPauseOrDispose { isRunning.value = false }
        }
        LaunchedEffect(isRunning) {
            while (isRunning.value) {
                timeSinceStartedInSeconds.value = (System.currentTimeMillis() - session.timeStarted) / 1000
                delay(1000)
            }
        }

        Row {
            Text(text = timeSinceStartedInSeconds.longValue.toString())
            Text(" seconds")
        }
        Row {
            Text("Outgoing packets: ")
            Text(text = session.outgoingPackets.intValue.toString())
        }
        Row {
            Text("Outgoing bytes: ")
            Text(text = session.outgoingBytes.intValue.toString())
        }
        Row {
            Text("Incoming packets: ")
            Text(text = session.incomingPackets.intValue.toString())
        }
        Row {
            Text("Incoming bytes: ")
            Text(text = session.incomingBytes.intValue.toString())
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ExpandedSessionItemPreview() {
    ExpandedSessionItem(
        Session(
            sourceAddress = "127.0.0.1",
            sourcePort = 8456,
            destinationAddress = "8.8.8.8",
            destinationPort = 443,
            protocol = "TCP",
            timeStarted = System.currentTimeMillis() - 1000
        )
    )
}