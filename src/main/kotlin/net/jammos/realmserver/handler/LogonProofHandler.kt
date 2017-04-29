package net.jammos.realmserver.handler

import com.google.common.base.Preconditions.checkArgument
import net.jammos.realmserver.auth.AuthCommand
import net.jammos.realmserver.auth.AuthDao
import net.jammos.realmserver.auth.AuthResult
import net.jammos.realmserver.auth.User
import net.jammos.realmserver.auth.crypto.CryptoManager
import net.jammos.realmserver.data.AuthLogonProofClient
import net.jammos.realmserver.data.AuthLogonProofResponse
import net.jammos.realmserver.data.AuthLogonProofResponseSuccessData
import net.jammos.realmserver.realms.RealmDao
import net.jammos.realmserver.utils.extensions.digest
import net.jammos.realmserver.utils.types.BigUnsignedInteger
import java.io.DataInput
import java.io.DataOutput

class LogonProofHandler(
        override val input: DataInput,
        val output: DataOutput,
        val build: Int,
        val user: User, // TODO: reload from DAO?
        val B: BigUnsignedInteger,
        val bSecret: BigUnsignedInteger,

        private val cryptoManager: CryptoManager,
        private val authDao: AuthDao,
        private val realmDao: RealmDao): Handler {

    private val N = cryptoManager.constants.N

    override fun handle(): Handler? {
        val sha1 = cryptoManager.sha1()

        requireCommand(AuthCommand.LOGON_PROOF)

        val proof = AuthLogonProofClient.read(input)
        System.out.println(proof)

        checkArgument(!((proof.A % N).isZero),
            "SRP safeguard abort == 0")

        val u = BigUnsignedInteger(sha1.digest(proof.A.bytes, B.bytes))
        val S = (proof.A * user.verifier.expMod(u, N)).expMod(bSecret, N)

        val K = cryptoManager.hashSessionKey(S)
        val M1s = cryptoManager.M1(
                user.username.toByteArray(Charsets.UTF_8),
                user.salt,
                proof.A,
                B,
                K)

        // Fail
        if (proof.M1 != M1s) {
            AuthLogonProofResponse(AuthResult.INCORRECT_PASSWORD).write(output)
            return null
        }

        // Ok!
        val M2 = sha1.digest(
                proof.A.bytes,
                proof.M1.bytes,
                K.bytes)

        AuthLogonProofResponse(AuthResult.SUCCESS,
                successData = AuthLogonProofResponseSuccessData(M2))
                .write(output)

        return RealmListHandler(input, output,
                username = user.username,

                authDao = authDao,
                realmDao = realmDao)
    }
}