package net.jammos.realmserver.network.step

import mu.KLogging
import net.jammos.realmserver.auth.AuthManager
import net.jammos.realmserver.auth.SuspendedException
import net.jammos.realmserver.auth.UnknownUserException
import net.jammos.realmserver.auth.Username.Username.username
import net.jammos.realmserver.network.message.client.ClientAuthLogonChallengeMessage
import net.jammos.realmserver.network.AuthResult
import net.jammos.realmserver.network.message.server.ServerAuthLogonChallengeResponse
import net.jammos.realmserver.network.message.server.ServerAuthLogonChallengeResponse.SuccessData
import net.jammos.realmserver.realm.RealmDao
import net.jammos.realmserver.utils.ByteArrays

class LogonChallengeStep(
        private val authManager: AuthManager,
        private val realmDao: RealmDao) : Step<ClientAuthLogonChallengeMessage, ServerAuthLogonChallengeResponse>(ClientAuthLogonChallengeMessage::class) {

    companion object : KLogging() {
        private fun errorResponse(authResult: AuthResult): ResponseAndNextStep<ServerAuthLogonChallengeResponse> {
            return ResponseAndNextStep(ServerAuthLogonChallengeResponse(authResult))
        }
    }

    override fun handle0(msg: ClientAuthLogonChallengeMessage): ResponseAndNextStep<ServerAuthLogonChallengeResponse> {
        // get identity
        val I = msg.srpIdentity
        val username = username(I)

        logger.debug { "Handling logon challenge for username($username)" }

        // challenge logon
        val proofDemand = try {
             authManager.challengeLogon(username, msg.ip)

        // unknown user?
        } catch (e: UnknownUserException) {
            logger.info { "Unknown user attempted logon: $username" }
            return errorResponse(AuthResult.UNKNOWN_ACCOUNT)

        // suspended user?
        } catch (e: SuspendedException) {
            logger.info { "Suspended user attempted logon by $username - ${e.message}" }

            return when(e.temporary) {
                true -> errorResponse(AuthResult.SUSPENDED)
                false -> errorResponse(AuthResult.BANNED)
            }
        }

        // create response (a proof demand)
        val response = ServerAuthLogonChallengeResponse(AuthResult.SUCCESS,
                successData = SuccessData(
                        g = proofDemand.g,
                        N = proofDemand.N,
                        B = proofDemand.B,
                        s = proofDemand.s,
                        unk3 = ByteArrays.randomBytes(16),
                        securityFlags = 0))

        // next step is for the client to provide proof
        logger.debug { "Happy with challenge for I{$I}, now I demand proof"}

        return ResponseAndNextStep(
                response = response,
                nextStep = LogonProofStep(
                        user = proofDemand.user,
                        B = proofDemand.B,
                        bSecret = proofDemand.bSecret,

                        authManager = authManager,
                        realmDao = realmDao
                )
        )
    }



}

