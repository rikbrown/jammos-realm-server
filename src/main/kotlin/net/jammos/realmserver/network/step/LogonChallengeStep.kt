package net.jammos.realmserver.network.step

import mu.KLogging
import net.jammos.realmserver.auth.AuthDao
import net.jammos.realmserver.network.AuthResult
import net.jammos.realmserver.auth.Username
import net.jammos.realmserver.auth.crypto.CryptoManager
import net.jammos.realmserver.data.ClientAuthLogonChallengeMessage
import net.jammos.realmserver.network.message.server.ServerAuthLogonChallengeResponse
import net.jammos.realmserver.network.message.server.ServerAuthLogonChallengeResponse.SuccessData
import net.jammos.realmserver.realms.RealmDao
import net.jammos.realmserver.utils.ByteArrays
import net.jammos.realmserver.utils.types.BigUnsignedInteger

class LogonChallengeStep(
        private val cryptoManager: CryptoManager,
        private val authDao: AuthDao,
        private val realmDao: RealmDao) : Step<ClientAuthLogonChallengeMessage, ServerAuthLogonChallengeResponse>(ClientAuthLogonChallengeMessage::class) {

    companion object : KLogging() {
        private fun errorResponse(authResult: AuthResult): ResponseAndNextStep<ServerAuthLogonChallengeResponse> {
            return ResponseAndNextStep(ServerAuthLogonChallengeResponse(authResult))
        }
    }

    private val k = cryptoManager.constants.k
    private val g = cryptoManager.constants.g
    private val N = cryptoManager.constants.N

    override fun handle0(msg: ClientAuthLogonChallengeMessage): ResponseAndNextStep<ServerAuthLogonChallengeResponse> {
        // get identity
        val I = msg.srpIdentity

        logger.debug { "Handling logon challenge for I($I)" }

        // retrieve user
        val user = authDao.getUser(Username.username(I)) ?:
                // unknown account?
                return errorResponse(AuthResult.UNKNOWN_ACCOUNT)

        // banned?
        if (user.isBanned) {
            return errorResponse(AuthResult.BANNED)
        }

        // calculate proof
        val b = BigUnsignedInteger.random(19) // secret ephemeral value
        val v = user.verifier
        val s = user.salt
        val B = ((k * v) + g.expMod(b, N)) % N // public ephemeral value
        val unk3 = ByteArrays.randomBytes(16)

        // create response
        val response = ServerAuthLogonChallengeResponse(AuthResult.SUCCESS,
                successData = SuccessData(
                        g = g,
                        N = N,
                        B = B,
                        s = s,
                        unk3 = unk3,
                        securityFlags = 0))

        // next step is for the client to provide proof
        logger.debug { "Happy with challenge for I{$I}, now I want proof"}
        return ResponseAndNextStep(
                response = response,
                nextStep = LogonProofStep(
                        user = user,
                        B = B,
                        bSecret = b,

                        cryptoManager = cryptoManager,
                        authDao = authDao,
                        realmDao = realmDao
                )
        )
    }



}

