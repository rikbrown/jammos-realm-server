package net.jammos.realmserver.auth

import com.google.common.base.Preconditions.checkArgument
import mu.KLogging
import net.jammos.realmserver.auth.crypto.CryptoManager
import net.jammos.realmserver.utils.extensions.digest
import net.jammos.realmserver.utils.types.BigUnsignedInteger
import java.net.InetAddress
import java.nio.charset.StandardCharsets.UTF_8

class AuthManager(
        private val cryptoManager: CryptoManager,
        private val authDao: AuthDao) {

    companion object : KLogging()

    private val k = cryptoManager.constants.k
    private val g = cryptoManager.constants.g
    private val N = cryptoManager.constants.N
    private val sha1 = cryptoManager.sha1()

    fun getUser(username: Username) = authDao.getUser(username)

    fun challengeLogon(
            username: Username,
            ip: InetAddress): ProofDemand {

        // banned IP?
        if (authDao.isIpBanned(ip)) {
            throw IpBannedException(ip)
        }

        // retrieve user
        val user = authDao.getUser(username) ?:
                // unknown account
                throw UnknownUserException(username)

        // banned or suspended? FIXME: cleaner?
        user.suspension
                ?.let { it.end == null }
                ?.let { throw UserSuspendedException(username, it) }

        // calculate proof
        val b = BigUnsignedInteger.random(19) // secret ephemeral value
        val v = user.verifier
        val s = user.salt
        val B = ((k * v) + g.expMod(b, N)) % N // public ephemeral value

        return ProofDemand(
                user = user,
                g = g,
                N = N,
                B = B,
                s = s,
                bSecret = b)
    }

    fun proofLogon(
            user: User, // TODO: reload from DAO?
            B: BigUnsignedInteger,
            bSecret: BigUnsignedInteger,
            A: BigUnsignedInteger,
            M1: BigUnsignedInteger): M2ByteArray? {

        checkArgument(!((A % N).isZero), "SRP safeguard abort == 0")

        val u = BigUnsignedInteger(sha1.digest(A.bytes, B.bytes))
        val S = (A * user.verifier.expMod(u, N)).expMod(bSecret, N)

        val K = cryptoManager.hashSessionKey(S)
        val M1s = cryptoManager.M1(
                user.username.toByteArray(UTF_8),
                user.salt,
                A,
                B,
                K)

        // Password match fail :(
        if (M1 != M1s) {
            logger.info { "Password mismatch for ${user.username}" }
            return null
        }

        // Ok!
        return M2ByteArray(sha1.digest(
                A.bytes,
                M1.bytes,
                K.bytes))
    }
}

data class ProofDemand(
        val user: User,
        val g: BigUnsignedInteger,
        val N: BigUnsignedInteger,
        val B: BigUnsignedInteger,
        val s: ByteArray,
        val bSecret: BigUnsignedInteger
)

// TODO:delegate?
data class M2ByteArray(val m2: ByteArray)

class IpBannedException(ip: InetAddress): RuntimeException("IP bsnned: $ip")
class UnknownUserException(username: Username): RuntimeException("Unknown username: $username")
class UserSuspendedException(username: Username, val temporary: Boolean): RuntimeException("User $username suspended temporary($temporary")