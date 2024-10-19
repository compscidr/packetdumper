package com.jasonernst.packetdumper.serverdumper

interface ConnectedUsersChangedCallback {
    fun onConnectedUsersChanged(connectedUsers: List<String>)
}
