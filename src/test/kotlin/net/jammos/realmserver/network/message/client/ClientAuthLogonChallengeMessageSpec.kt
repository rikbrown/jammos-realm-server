package net.jammos.realmserver.network.message.client

import com.google.common.io.ByteStreams
import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should equal`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.io.DataInput
import javax.xml.bind.DatatypeConverter

object ClientAuthLogonChallengeMessageSpec : Spek({
    val CHALLENGE_BYTES = DatatypeConverter.parseHexBinary("00032600576f5700010c01f3163638780058534f0053556e65a8fdffff7f0000010852494b42524f574e")

    given("a challenge") {
        val challenge = { ByteStreams.newDataInput(CHALLENGE_BYTES) }

        on("readBody") {
            val authMessage = ClientAuthLogonChallengeMessage.readBody(challenge().dropOneByte())

            it("should read correct fields") {
                authMessage.gameName `should equal` "WoW"
                authMessage.version1 `should equal` 1
                authMessage.version2 `should equal` 12
                authMessage.version3 `should equal` 1
                authMessage.build `should equal` 62230
                authMessage.platform `should equal` "x86"
                authMessage.os `should equal` "OSX"
                authMessage.country `should equal` "enUS"
                authMessage.timezoneBias `should equal` -1459748865
                authMessage.ip.toString() `should equal` "/127.0.0.1"
                authMessage.srpIdentity `should equal` "RIKBROWN"
            }
        }

        on("ClientAuthMessage.read") {
            val authMessage = ClientAuthMessage.read(challenge())
            it("should read the correct object") {
                authMessage `should be instance of` ClientAuthLogonChallengeMessage::class
            }
        }
    }
})

/**
 * drop a byte to account for the command which is read earlier
 */
fun DataInput.dropOneByte(): DataInput {
    readByte()
    return this
}


/*
2017-05-19 09:00:05 [DEBUG] [nioEventLoopGroup-3-1    ] ReadLogging:59 - gameName = WoW
2017-05-19 09:00:05 [DEBUG] [nioEventLoopGroup-3-1    ] ReadLogging:65 - version1 = 1
2017-05-19 09:00:05 [DEBUG] [nioEventLoopGroup-3-1    ] ReadLogging:71 - version2 = 12
2017-05-19 09:00:05 [DEBUG] [nioEventLoopGroup-3-1    ] ReadLogging:77 - version3 = 1
2017-05-19 09:00:05 [DEBUG] [nioEventLoopGroup-3-1    ] ReadLogging:83 - build = 62230
2017-05-19 09:00:05 [DEBUG] [nioEventLoopGroup-3-1    ] ReadLogging:89 - platform = x86
2017-05-19 09:00:05 [DEBUG] [nioEventLoopGroup-3-1    ] ReadLogging:95 - os = OSX
2017-05-19 09:00:05 [DEBUG] [nioEventLoopGroup-3-1    ] ReadLogging:101 - country = enUS
2017-05-19 09:00:05 [DEBUG] [nioEventLoopGroup-3-1    ] ReadLogging:107 - timezoneBias = -1459748865
2017-05-19 09:00:05 [DEBUG] [nioEventLoopGroup-3-1    ] ReadLogging:113 - ip = /127.0.0.1
2017-05-19 09:00:05 [DEBUG] [nioEventLoopGroup-3-1    ] ReadLogging:121 - srpIdentityLength = 8
2017-05-19 09:00:05 [DEBUG] [nioEventLoopGroup-3-1    ] ReadLogging:133 - srpIdentity = RIKBROWN

 */