package net.jammos.realmserver.network.message.client

import com.google.common.io.ByteStreams
import net.jammos.realmserver.test.utils.dropOneByte
import net.jammos.utils.types.BigUnsignedInteger
import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should equal`
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import javax.xml.bind.DatatypeConverter.parseHexBinary

object ClientAuthLogonProofMessageSpec: Spek({
    val CHALLENGE_BYTES = parseHexBinary("0195f7ecfad4434886f83c74a01e744a970ef35f1d3977019155f3f0c7616e0c249e77a7c97a52e2e32a8752bb7d2e2522e613248eac961cd3b11888f0ac348bb04b3aad66e4ce2e4a0000")

    given("a challenge") {
        val challenge = { ByteStreams.newDataInput(CHALLENGE_BYTES) }

        on("readBody") {
            val authMessage = ClientAuthLogonProofMessage.readBody(challenge().dropOneByte())

            it("should read correct fields") {
                authMessage.A `should equal` BigUnsignedInteger.ofHexString("95F7ECFAD4434886F83C74A01E744A970EF35F1D3977019155F3F0C7616E0C24")
                authMessage.M1 `should equal` BigUnsignedInteger.ofHexString("9E77A7C97A52E2E32A8752BB7D2E2522E613248E")
                authMessage.crc_hash `should equal` parseHexBinary("AC961CD3B11888F0AC348BB04B3AAD66E4CE2E4A")
                authMessage.numberOfKeys `should equal` 0
                authMessage.securityFlags `should equal` 0
            }
        }

        on("ClientAuthMessage.read") {
            val authMessage = ClientAuthMessage.read(challenge())
            it("should read the correct object") {
                authMessage `should be instance of` ClientAuthLogonProofMessage::class
            }
        }
    }

})

