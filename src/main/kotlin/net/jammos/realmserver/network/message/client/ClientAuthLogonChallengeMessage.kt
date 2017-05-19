package net.jammos.realmserver.network.message.client

import net.jammos.realmserver.utils.extensions.readChars
import net.jammos.realmserver.utils.extensions.readIpAddress
import net.jammos.realmserver.utils.extensions.readUnsignedInt
import net.jammos.realmserver.utils.field
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
        override fun readBody(input: DataInput): ClientAuthLogonChallengeMessage {
            field("error") { input.readByte() }
            field("packetSize") { input.readUnsignedShort() }

            return with(input) {
                // @formatter:off
                ClientAuthLogonChallengeMessage(
                        gameName     = field("gameName")     { readChars(4, reverse = false) },
                        version1     = field("version1")     { readUnsignedByte() },
                        version2     = field("version2")     { readUnsignedByte() },
                        version3     = field("version3")     { readUnsignedByte() },
                        build        = field("build")        { readUnsignedShort() },
                        platform     = field("platform")     { readChars(4) },
                        os           = field("os")           { readChars(4) },
                        country      = field("country")      { readChars(4) },
                        timezoneBias = field("timezoneBias") { readUnsignedInt() },
                        ip           = field("ip")           { readIpAddress() },

                        srpIdentity  = field("srpIdentity")  {
                            readChars(field("srpIdentityLength") { readUnsignedByte() },
                            reverse = false) }
                )
                // @formatter:on
            }
        }

    }

}

