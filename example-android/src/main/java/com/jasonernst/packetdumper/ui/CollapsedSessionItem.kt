package com.jasonernst.packetdumper.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.jasonernst.packetdumper.Session

@Composable
fun CollapsedSessionItem(session: Session) {
    Column {
        Row {
            Text(text = session.destinationAddress)
            Text(":")
            Text(text = session.destinationPort.toString())
            Text(":")
            Text(text = session.protocol)
            Text("Out: ")
            Text(text = session.outgoingBytes.intValue.toString())
            Text("b")
            Text(" In: ")
            Text(text = session.incomingBytes.intValue.toString())
            Text("b")
        }
    }
}