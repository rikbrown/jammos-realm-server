package net.jammos.realmserver.network.message.client

import com.google.common.io.ByteStreams
import net.jammos.realmserver.test.utils.dropOneByte
import org.amshove.kluent.`should be instance of`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import javax.xml.bind.DatatypeConverter

object ClientRealmListMessageSpec : Spek({
    val CHALLENGE_BYTES = DatatypeConverter.parseHexBinary("1000000000")

    given("a challenge") {
        val challenge = { ByteStreams.newDataInput(CHALLENGE_BYTES) }

        on("readBody") {
            it("should read ok") {
                ClientRealmListMessage.readBody(challenge().dropOneByte())
            }
        }

        on("ClientAuthMessage.read") {
            val authMessage = ClientAuthMessage.read(challenge())
            it("should read the correct object") {
                authMessage `should be instance of` ClientRealmListMessage::class
            }
        }
    }
})


