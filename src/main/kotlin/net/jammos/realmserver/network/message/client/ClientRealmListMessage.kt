package net.jammos.realmserver.network.message.client

import java.io.DataInput

class ClientRealmListMessage: ClientAuthMessage {

    companion object : ClientAuthMessage.Reader {
        val instance = ClientRealmListMessage()

        override fun readBody(input: DataInput): ClientRealmListMessage {
            input.skipBytes(4) // skip 4 zero bytes
            return instance
        }
    }

}

