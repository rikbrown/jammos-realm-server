package net.jammos.realmserver.network.message.client

import mu.KLogging
import net.jammos.realmserver.network.AuthCommand
import net.jammos.realmserver.data.ClientAuthLogonChallengeMessage
import java.io.DataInput

interface ClientAuthMessage {
    companion object: KLogging() {
        private val lookup = mapOf(
                AuthCommand.LOGON_CHALLENGE to ClientAuthLogonChallengeMessage.Companion,
                AuthCommand.LOGON_PROOF to ClientAuthLogonProofMessage.Companion,
                AuthCommand.REALM_LIST to ClientRealmListMessage.Companion)

        fun read(input: DataInput): ClientAuthMessage {
            // Read auth command
            val authCommand = AuthCommand.read(input) ?: throw RuntimeException("Unknown command")
            logger.debug { "Read authCommand($authCommand)"}

            val handler = lookup[authCommand] ?: throw RuntimeException("Unsupported command")

            // Delegate rest to the individual reader
            logger.debug { "Delegating to $handler" }

            val message = handler.readBody(input)
            logger.debug { "Read body $message" }

            return message
        }
    }

    interface Reader {
        fun readBody(input: DataInput): ClientAuthMessage
    }

}

