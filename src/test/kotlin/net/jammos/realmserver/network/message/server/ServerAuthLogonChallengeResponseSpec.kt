package net.jammos.realmserver.network.message.server

import com.google.common.io.ByteStreams
import net.jammos.realmserver.network.AuthCommand
import net.jammos.realmserver.network.AuthResult
import net.jammos.utils.types.BigUnsignedInteger
import org.amshove.kluent.`should equal`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object ServerAuthLogonChallengeResponseSpec: Spek({
    given("a failed response") {
        val result = AuthResult.BANNED
        val response = ServerAuthLogonChallengeResponse(result)

        on("write") {
            val bytes = with(ByteStreams.newDataOutput()) {
                response.write(this)
                ByteStreams.newDataInput(this.toByteArray())
            }

            it("should output basic error response") {
                bytes.readUnsignedByte() `should equal` AuthCommand.LOGON_CHALLENGE.value
                bytes.readUnsignedByte() `should equal` 0x00
                bytes.readUnsignedByte() `should equal` result.value
            }
        }
    }

    given("a successful response") {
        val B = BigUnsignedInteger.random(32)
        val g = BigUnsignedInteger.random(1)
        val N = BigUnsignedInteger.random(32)
        val s = BigUnsignedInteger.random(32)
        val unk3 = BigUnsignedInteger.random(16)
        val sv = 0
    }

})