package com.jasonernst.example_android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jasonernst.kanonproxy.Session
import java.net.InetAddress

@Composable
fun SessionItem(session: Session) {
    Row {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.SpaceBetween) {
            Row {
                Text("Source: ")
                Text(text = session.sourceAddress.hostAddress ?: "Unknown")
                Text(":")
                Text(text = session.sourcePort.toString())
            }
            Row {
                Text("Destination: ")
                Text(text = session.destinationAddress.hostAddress ?: "Unknown")
                Text(":")
                Text(text = session.destinationPort.toString())
            }
        }
        Column {
            Row(modifier = Modifier.align(Alignment.End)) {
                // todo: convert the protocol to the enum type name
                Text(text = session.protocol.toString())
            }
//            val timeSinceStartedInSeconds = (System.currentTimeMillis() - session.timeStarted) / 1000
//            Row {
//                Text("Active for: ")
//                Text(text = timeSinceStartedInSeconds.toString())
//                Text(" seconds")
//            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SessionItemPreview() {
//    SessionItem(
//        Session(
//            sourceAddress = InetAddress.getByName("127.0.0.1"),
//            sourcePort = 8456u,
//            destinationAddress = InetAddress.getByName("8.8.8.8"),
//            destinationPort = 443u,
//            protocol = 6u,
//            timeStarted = System.currentTimeMillis() - 1000
//        )
//    )
}