package net.jammos.realmserver.network.step

import com.google.common.base.Preconditions.checkArgument
import mu.KLogging
import net.jammos.realmserver.auth.AuthDao
import net.jammos.realmserver.network.AuthResult
import net.jammos.realmserver.auth.User
import net.jammos.realmserver.auth.crypto.CryptoManager
import net.jammos.realmserver.network.message.client.ClientAuthLogonProofMessage
import net.jammos.realmserver.network.message.server.ServerAuthLogonProofResponse
import net.jammos.realmserver.realms.RealmDao
import net.jammos.realmserver.utils.extensions.digest
import net.jammos.realmserver.utils.types.BigUnsignedInteger


class LogonProofStep(
        private val user: User, // TODO: reload from DAO?
        private val B: BigUnsignedInteger,
        private val bSecret: BigUnsignedInteger,

        private val cryptoManager: CryptoManager,
        private val authDao: AuthDao,
        private val realmDao: RealmDao) : Step<ClientAuthLogonProofMessage, ServerAuthLogonProofResponse>(ClientAuthLogonProofMessage::class) {

    companion object : KLogging() {
        private fun errorResponse(authResult: AuthResult): ResponseAndNextStep<ServerAuthLogonProofResponse> {
            return ResponseAndNextStep(ServerAuthLogonProofResponse(authResult))
        }
    }

    private val N = cryptoManager.constants.N
    private val sha1 = cryptoManager.sha1()

    override fun handle0(msg: ClientAuthLogonProofMessage): ResponseAndNextStep<ServerAuthLogonProofResponse> {
        logger.debug { "Handling logon proof for user(${user.username})" }

        checkArgument(!((msg.A % N).isZero), "SRP safeguard abort == 0")

        val u = BigUnsignedInteger(sha1.digest(msg.A.bytes, B.bytes))
        val S = (msg.A * user.verifier.expMod(u, N)).expMod(bSecret, N)

        val K = cryptoManager.hashSessionKey(S)
        val M1s = cryptoManager.M1(
                user.username.toByteArray(Charsets.UTF_8),
                user.salt,
                msg.A,
                B,
                K)

        // Password match fail :(
        if (msg.M1 != M1s) {
            logger.info { "Password mismatch for ${user.username}" }
            return errorResponse(AuthResult.INCORRECT_PASSWORD)
        }

        // Ok!
        val M2 = sha1.digest(
                msg.A.bytes,
                msg.M1.bytes,
                K.bytes)

        // Create response
        val response = ServerAuthLogonProofResponse(AuthResult.SUCCESS,
                successData = ServerAuthLogonProofResponse.SuccessData(M2))

        // next step is realm list! :)
        logger.debug { "Happy with proof for user(${user.username})"}
        return ResponseAndNextStep(
                response = response,
                nextStep = RealmListStep(
                        username = user.username,

                        authDao = authDao,
                        realmDao = realmDao)
        )

    }


}