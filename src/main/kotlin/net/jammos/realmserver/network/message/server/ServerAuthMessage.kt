package net.jammos.realmserver.network.message.server

import java.io.DataOutput

interface ServerAuthMessage {
    fun write(output: DataOutput)
}