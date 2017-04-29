package net.jammos.realmserver.handler

import net.jammos.realmserver.auth.AuthCommand
import net.jammos.realmserver.auth.AuthDao
import net.jammos.realmserver.auth.AuthResult
import net.jammos.realmserver.auth.Username.Username.username
import net.jammos.realmserver.auth.crypto.CryptoManager
import net.jammos.realmserver.data.AuthLogonChallenge
import net.jammos.realmserver.data.AuthLogonChallengeResponse
import net.jammos.realmserver.data.AuthLogonChallengeResponseSuccessData
import net.jammos.realmserver.realms.RealmDao
import net.jammos.realmserver.utils.ByteArrays.randomBytes
import net.jammos.realmserver.utils.types.BigUnsignedInteger
import java.io.DataInput
import java.io.DataOutput

class LogonChallengeHandler(
        override val input: DataInput,
        val output: DataOutput,

        private val cryptoManager: CryptoManager,
        private val authDao: AuthDao,
        private val realmDao: RealmDao): Handler {

    private val k = cryptoManager.constants.k
    private val g = cryptoManager.constants.g
    private val N = cryptoManager.constants.N

    override fun handle(): Handler? {
        requireCommand(AuthCommand.LOGON_CHALLENGE)
        val error = input.readByte()
        val packetSize = input.readUnsignedShort()

        val challenge = AuthLogonChallenge.read(input)
        val I = challenge.srpIdentity

        val user = authDao.getUser(username(I))

        // unknown account?
        if (user == null) {
            AuthLogonChallengeResponse(AuthResult.UNKNOWN_ACCOUNT).write(output)
            return null
        }

        // banned?
        if (user.isBanned) {
            AuthLogonChallengeResponse(AuthResult.BANNED).write(output)
            return null
        }

        val b = BigUnsignedInteger.random(19) // secret ephemeral value
        val v = user.verifier
        val s = user.salt

        val B = ((k * v) + g.expMod(b, N)) % N // public ephemeral value

        val unk3 = randomBytes(16)

        val response = AuthLogonChallengeResponse(AuthResult.SUCCESS,
                successData = AuthLogonChallengeResponseSuccessData(
                        g = g,
                        N = N,

                        B = B,
                        s = s,
                        unk3 = unk3,
                        securityFlags = 0))
        System.out.println(response)
        response.write(output)

        return LogonProofHandler(input, output,
                build = challenge.build,
                user = user,
                B = B,
                bSecret = b,

                cryptoManager = cryptoManager,
                authDao = authDao,
                realmDao = realmDao)
    }

}

