package net.jammos.realmserver.network.message.client

import com.google.common.collect.Maps.immutableEnumMap
import io.netty.buffer.ByteBuf
import mu.KLogging
import net.jammos.realmserver.network.AuthCommand

interface ClientAuthMessage {
    companion object: KLogging() {
        private val lookup = immutableEnumMap(mapOf(
                AuthCommand.LOGON_CHALLENGE to ClientAuthLogonChallengeMessage.Reader(isReconnect = false),
                AuthCommand.LOGON_PROOF to ClientAuthLogonProofMessage,
                AuthCommand.REALM_LIST to ClientRealmListMessage,

                AuthCommand.RECONNECT_CHALLENGE to ClientAuthLogonChallengeMessage.Reader(isReconnect = true)))

        fun read(input: ByteBuf): ClientAuthMessage {
            // Read auth command
            val authCommand = AuthCommand.read(input)
            logger.debug { "Read authCommand($authCommand)"}

            val handler = lookup[authCommand] ?: throw IllegalArgumentException("Unsupported command")

            // Delegate rest to the individual reader
            logger.debug { "Delegating to $handler" }

            val message = handler.readBody(input)
            logger.debug { "Read body $message" }

            return message
        }
    }

    interface Reader {
        fun readBody(input: ByteBuf): ClientAuthMessage
    }

}

