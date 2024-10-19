package com.jasonernst.packetdumper.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jasonernst.packetdumper.Session

@Composable
fun SessionItem(session: Session) {
    val isExpanded = remember { mutableStateOf(false) }
    Row(modifier = Modifier.padding(top = 12.dp).fillMaxWidth().clickable(onClick = { isExpanded.value = !isExpanded.value })) {
        if (isExpanded.value) {
            ExpandedSessionItem(session)
        } else {
            CollapsedSessionItem(session)
        }
    }
}