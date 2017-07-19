package net.jammos.realmserver.auth

import mu.KLogging
import net.jammos.realmserver.auth.crypto.CryptoManager
import net.jammos.realmserver.utils.checkArgument
import net.jammos.realmserver.utils.extensions.digest
import net.jammos.realmserver.utils.types.BigUnsignedInteger
import net.jammos.realmserver.utils.types.ComparableByteArray
import org.omg.CORBA.UnknownUserException
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

    /**
     * Starts a logon challenge for [username] from [ip].
     *
     * @return If challenge request is accepted, an [AuthChallenge] will be returned for the client to
     * respond to
     * @throws UnknownUserException if the [username] was not recognised
     * @throws UserSuspendedException if the [username] was suspended/banned
     */
    fun challengeLogon(
            username: Username,
            ip: InetAddress): AuthChallenge {

        // banned IP?
        authDao.getIpSuspension(ip)
                ?.let { it.end != null }
                ?.let { throw IpBannedException(ip, temporary = it) }

        // retrieve user
        val user = authDao.getUser(username) ?:
                // unknown account
                throw UnknownUserException(username)

        // banned or suspended?
        user.suspension
                ?.let { it.end != null }
                ?.let { throw UserSuspendedException(username, temporary = it) }

        // calculate proof
        val b = BigUnsignedInteger.random(19) // secret ephemeral value
        val v = user.verifier
        val s = user.salt
        val B = ((k * v) + g.expMod(b, N)) % N // public ephemeral value

        return AuthChallenge(
                user = user,
                g = g,
                N = N,
                B = B,
                s = s,
                bSecret = b)
    }

    /**
     * Verifies a logon attempt for the given [user] using the provided proof values.
     *
     * @return If successful, the [M2ByteArray] will be returned.  If unsuccessful, null will
     * be returned.
     */
    fun proofLogon(
            user: User, // TODO: reload from DAO?
            B: BigUnsignedInteger,
            bSecret: BigUnsignedInteger,
            A: BigUnsignedInteger,
            M1: BigUnsignedInteger): M2ByteArray? {

        checkArgument(!((A % N).isZero)) { "SRP safeguard abort == 0" }

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

data class AuthChallenge(
        val user: User,
        val g: BigUnsignedInteger,
        val N: BigUnsignedInteger,
        val B: BigUnsignedInteger,
        val s: SaltByteArray,
        val bSecret: BigUnsignedInteger
)

class M2ByteArray(bytes: ByteArray): ComparableByteArray(bytes)
class SaltByteArray(bytes: ByteArray): ComparableByteArray(bytes)

sealed class SuspendedException(message: String, val temporary: Boolean): RuntimeException("$message (temporary=$temporary)")
class IpBannedException(ip: InetAddress, temporary: Boolean): SuspendedException("IP banned: $ip", temporary)
class UserSuspendedException(username: Username, temporary: Boolean): SuspendedException("User suspended: $username", temporary)

class UnknownUserException(username: Username): RuntimeException("Unknown username: $username")