package net.jammos.realmserver.network.message.client

import net.jammos.realmserver.utils.checkArgument
import net.jammos.realmserver.utils.extensions.readChars
import net.jammos.realmserver.utils.extensions.readIpAddress
import net.jammos.realmserver.utils.extensions.readUnsignedInt
import java.io.DataInput
import java.net.InetAddress

data class ClientAuthLogonChallengeMessage(
        val gameName: String,
        val version1: Int,
        val version2: Int,
        val version3: Int,
        val build: Int,
        val platform: String,
        val os: String,
        val country: String,
        val timezoneBias: Int,
        val ip: InetAddress,
        val srpIdentity: String

): ClientAuthMessage {

    companion object : ClientAuthMessage.Reader {
        private val SRP_IDENTITY_SIZE_PREDICATE = { size: Int -> size < 20 }

        @Suppress("UNUSED_VARIABLE") // keeping error/packetSize for reference
        override fun readBody(input: DataInput): ClientAuthLogonChallengeMessage {
            val error = input.readByte()
            val packetSize = input.readUnsignedShort()

            return ClientAuthLogonChallengeMessage(
                    gameName = input.readChars(4, reverse = false),
                    version1 = input.readUnsignedByte(),
                    version2 = input.readUnsignedByte(),
                    version3 = input.readUnsignedByte(),
                    build = input.readUnsignedShort(),
                    platform = input.readChars(4),
                    os = input.readChars(4),
                    country = input.readChars(4),
                    timezoneBias = input.readUnsignedInt(),
                    ip = input.readIpAddress(),
                    // FIXME: validate the length isn't ridiculous
                    srpIdentity = input.readChars(
                            checkArgument(input.readUnsignedByte(), SRP_IDENTITY_SIZE_PREDICATE) { "srp identity too long ($it bytes)" },
                            reverse = false))
        }

    }

}

