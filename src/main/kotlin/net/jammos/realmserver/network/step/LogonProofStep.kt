package net.jammos.realmserver.network.step

import mu.KLogging
import net.jammos.realmserver.auth.AuthManager
import net.jammos.realmserver.auth.UserAuth
import net.jammos.realmserver.network.AuthResult
import net.jammos.realmserver.network.message.client.ClientAuthLogonProofMessage
import net.jammos.realmserver.network.message.server.ServerAuthLogonProofResponse
import net.jammos.realmserver.realm.RealmDao
import net.jammos.realmserver.utils.types.BigUnsignedInteger


class LogonProofStep(
        private val userAuth: UserAuth, // TODO: reload from DAO?
        private val B: BigUnsignedInteger,
        private val bSecret: BigUnsignedInteger,

        private val authManager: AuthManager,
        private val realmDao: RealmDao) : Step<ClientAuthLogonProofMessage, ServerAuthLogonProofResponse>(ClientAuthLogonProofMessage::class) {

    companion object : KLogging() {
        private fun errorResponse(authResult: AuthResult): ResponseAndNextStep<ServerAuthLogonProofResponse> {
            return ResponseAndNextStep(ServerAuthLogonProofResponse(authResult))
        }
    }

    override fun handle0(msg: ClientAuthLogonProofMessage): ResponseAndNextStep<ServerAuthLogonProofResponse> {
        logger.debug { "Handling logon proof for user ${userAuth.username}" }

        val M2 = authManager.proofLogon(
                userAuth = userAuth,
                B = B,
                bSecret = bSecret,
                A = msg.A,
                M1 = msg.M1)

        // Proof fail (password mismatch)
        if (M2 == null) {
            logger.info { "Password mismatch for ${userAuth.username}" }
            return errorResponse(AuthResult.INCORRECT_PASSWORD)
        }

        // Create response
        val response = ServerAuthLogonProofResponse(AuthResult.SUCCESS,
                successData = ServerAuthLogonProofResponse.SuccessData(M2))

        // next step is realm list! :)
        logger.debug { "Happy with proof for user ${userAuth.username}"}
        return ResponseAndNextStep(
                response = response,
                nextStep = RealmListStep(
                        username = userAuth.username,

                        authManager = authManager,
                        realmDao = realmDao)
        )
    }


}