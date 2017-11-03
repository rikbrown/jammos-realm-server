package net.jammos.realmserver.network.message.client

import io.netty.buffer.ByteBuf

class ClientRealmListMessage: ClientAuthMessage {

    companion object : ClientAuthMessage.Reader {
        val instance = ClientRealmListMessage()

        override fun readBody(input: ByteBuf): ClientRealmListMessage {
            input.skipBytes(4) // skip 4 zero bytes
            return instance
        }
    }

}

