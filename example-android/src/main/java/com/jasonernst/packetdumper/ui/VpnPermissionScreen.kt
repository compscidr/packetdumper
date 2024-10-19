package com.jasonernst.packetdumper.ui

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.jasonernst.packetdumper.model.SessionViewModel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

@Composable
fun VPNPermissionScreen(sessionViewModel: SessionViewModel) {
    val context = LocalContext.current
    val logger = LoggerFactory.getLogger("VPNPermissionScreen")
    val scope = rememberCoroutineScope()
    val startForResult =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                logger.debug("VPN permission granted")
                sessionViewModel.hidePermissionScreen()
            } else {
                logger.debug("VPN permission denied")
                sessionViewModel.hidePermissionScreen()
            }
        }
    val requestVpnAction: () -> Unit = {
        startForResult.launch(VpnService.prepare(context))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(start = 24.dp, top = 54.dp, end=24.dp)) {
            Text("This app uses the VPN services to capture packets so that you can analyze them.")
        }
        Row(modifier = Modifier.padding(start = 24.dp, top = 10.dp, end=24.dp)) {
            Text("This is not compatible with other VPN services running at the same time.")
        }

        val annotatedString = buildAnnotatedString {
            append("Data is not sent to any external server, all traffic is routed to ")
            withLink(
                LinkAnnotation.Url(
                    url = "https://github.com/compscidr/kanonproxy",
                    TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary))
                )
            ) {
                append("kAnonProxy")
            }
            append(" which makes the requests to the servers on your behalf.")
        }
        Row(modifier = Modifier.padding(start = 24.dp, top = 10.dp, end=24.dp)) {
            Text(annotatedString)
        }
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(modifier = Modifier.padding(start = 24.dp, top = 10.dp, end=24.dp)) {
                Button(modifier = Modifier.width(200.dp), onClick = {
                    logger.debug("Requesting VPN permissions")
                    scope.launch {
                        requestVpnAction()
                    }
                }) {
                    Text(text = "Allow VPN Permission")
                }
            }
            Row {
                Button(modifier = Modifier.width(200.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary), onClick = {
                        logger.debug("Not now")
                        sessionViewModel.hidePermissionScreen()
                    }) {
                    Text(modifier = Modifier, text = "Not Now")
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally) {
            val annotatedString = buildAnnotatedString {
                withLink(
                    LinkAnnotation.Url(
                        url = "https://github.com/compscidr/packetdumper",
                        TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary))
                    )
                ) {
                    append("PacketDumper")
                }
                append(" is open source and available on GitHub.")
            }

            Row(modifier = Modifier.padding(start = 24.dp, bottom = 10.dp, end=24.dp)) {
                Text(annotatedString)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VPNPermissionScreenPreview() {
    val context = LocalContext.current
    val sessionViewModel = SessionViewModel.Companion.getInstance(PreferenceManager.getDefaultSharedPreferences(context))
    VPNPermissionScreen(sessionViewModel)
}