package net.jammos.realmserver.network.message.client

import io.netty.buffer.ByteBuf
import net.jammos.utils.extensions.readCharSequence
import net.jammos.utils.extensions.readIpAddress
import net.jammos.utils.field
import java.net.InetAddress

data class ClientAuthLogonChallengeMessage(
        val gameName: CharSequence,
        val version1: Short,
        val version2: Short,
        val version3: Short,
        val build: Int,
        val platform: CharSequence,
        val os: CharSequence,
        val country: CharSequence,
        val timezoneBias: Long,
        val ip: InetAddress,
        val srpIdentity: CharSequence,
        val isReconnect: Boolean

): ClientAuthMessage {

    class Reader(private val isReconnect: Boolean): ClientAuthMessage.Reader {
        override fun readBody(input: ByteBuf): ClientAuthLogonChallengeMessage {
            field("error") { input.readByte() }
            field("packetSize") { input.readUnsignedShort() }

            return with(input) {
                // @formatter:off
                ClientAuthLogonChallengeMessage(
                        isReconnect = isReconnect,

                        gameName     = field("gameName")     { readCharSequence(4) },
                        version1     = field("version1")     { readUnsignedByte() },
                        version2     = field("version2")     { readUnsignedByte() },
                        version3     = field("version3")     { readUnsignedByte() },
                        build        = field("build")        { readUnsignedShort() },
                        platform     = field("platform")     { readCharSequence(4) },
                        os           = field("os")           { readCharSequence(4) },
                        country      = field("country")      { readCharSequence(4) },
                        timezoneBias = field("timezoneBias") { readUnsignedInt() },
                        ip           = field("ip")           { readIpAddress() },

                        srpIdentity  = field("srpIdentity")  {
                            readCharSequence(field("srpIdentityLength") { readUnsignedByte().toInt() }) }
                )
                // @formatter:on
            }
        }

    }

}

