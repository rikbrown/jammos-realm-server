package net.jammos.realmserver.auth

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import mu.KLogging
import net.jammos.realmserver.auth.crypto.CryptoManager
import net.jammos.realmserver.utils.checkArgument
import net.jammos.realmserver.utils.extensions.digest
import net.jammos.realmserver.utils.extensions.minutes
import net.jammos.realmserver.utils.types.BigUnsignedInteger
import net.jammos.realmserver.utils.types.ComparableByteArray
import org.omg.CORBA.UnknownUserException
import java.net.InetAddress
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Duration
import java.time.Instant
import java.time.Instant.now

class AuthManager(
        private val cryptoManager: CryptoManager,
        private val authDao: AuthDao) {

    companion object: KLogging()

    private val k = cryptoManager.constants.k
    private val g = cryptoManager.constants.g
    private val N = cryptoManager.constants.N
    private val sha1 = cryptoManager.sha1()

    /**
     * Validates [username] ensuring it is a known and not suspended username
     *
     * @throws UnknownUserException if the user is not known
     * @throws UserSuspendedException if the user is suspended or banned
     */
    fun validateUser(username: Username): UserAuth {
        val userAuth = authDao.getUserAuth(username) ?:
                // unknown account
                throw UnknownUserException(username)

        authDao.getUserSuspension(username)
                ?.let { it.end != null }
                ?.let { throw UserSuspendedException(username, temporary = it) }

        return userAuth
    }

    /**
     * Starts a logon challenge for [username] from [ip].  Will also invoke validation via [validateUser].
     *
     * @return If challenge request is accepted, an [AuthChallenge] will be returned for the client to
     * respond to.
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

        // retrieve user and verify not suspended
        val userAuth = validateUser(username)

        // calculate proof
        val b = BigUnsignedInteger.random(19) // secret ephemeral value
        val v = userAuth.verifier
        val s = userAuth.salt
        val B = ((k * v) + g.expMod(b, N)) % N // public ephemeral value

        return AuthChallenge(
                userAuth = userAuth,
                g = g,
                N = N,
                B = B,
                s = s,
                bSecret = b)
    }

    /**
     * Verifies a logon attempt for the given [userAuth] using the provided proof values.
     *
     * @return If successful, the [M2ByteArray] will be returned.  If unsuccessful, null will
     * be returned.
     */
    fun proofLogon(
            userAuth: UserAuth, // TODO: reload from DAO?
            B: BigUnsignedInteger,
            bSecret: BigUnsignedInteger,
            A: BigUnsignedInteger,
            M1: BigUnsignedInteger): M2ByteArray? {

        checkArgument(!((A % N).isZero)) { "SRP safeguard abort == 0" }

        val u = BigUnsignedInteger(sha1.digest(A.bytes, B.bytes))
        val S = (A * userAuth.verifier.expMod(u, N)).expMod(bSecret, N)

        val K = cryptoManager.hashSessionKey(S)
        val M1s = cryptoManager.M1(
                userAuth.username.toByteArray(UTF_8),
                userAuth.salt,
                A,
                B,
                K)

        // Password match fail :(
        if (M1 != M1s) {
            logger.info { "Password mismatch for ${userAuth.username}" }
            launch(CommonPool) { handleAuthFailure(userAuth.username) } // record the failure and possibly suspend based on it
            return null
        }

        // Ok! Update session key
        authDao.updateUserSessionKey(userAuth.username, K)

        // and return M2
        return M2ByteArray(sha1.digest(
                A.bytes,
                M1.bytes,
                K.bytes))
    }

    suspend private fun handleAuthFailure(username: Username) {
        if (authDao.recordUserAuthFailure(username) > 5) {
            // User failed auth too many times, we should suspend
            // TODO: configuration options
            authDao.suspendUser(username, start = now(), end = now() + 15.minutes)
        }

    }
}

data class AuthChallenge(
        val userAuth: UserAuth,
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