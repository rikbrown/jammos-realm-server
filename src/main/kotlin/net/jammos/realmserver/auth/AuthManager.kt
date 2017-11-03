package net.jammos.realmserver.auth

import mu.KLogging
import net.jammos.utils.ByteArrays
import net.jammos.utils.auth.SaltByteArray
import net.jammos.utils.auth.UserAuth
import net.jammos.utils.auth.UserId
import net.jammos.utils.auth.Username
import net.jammos.utils.auth.crypto.CryptoManager
import net.jammos.utils.auth.dao.AuthDao
import net.jammos.utils.checkArgument
import net.jammos.utils.extensions.digest
import net.jammos.utils.extensions.minutes
import net.jammos.utils.types.BigUnsignedInteger
import net.jammos.utils.types.ComparableByteArray
import org.omg.CORBA.UnknownUserException
import java.net.InetAddress
import java.time.Instant.now

private const val SUSPEND_AFTER_LOGIN_FAILURES = 5
private val LOGIN_FAILURE_SUSPENSION_DURATION = 5.minutes

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
    fun validateUser(userId: UserId): UserAuth {
        val username = authDao.getUserUsername(userId) ?: throw UnknownUserException(userId)
        return validateUser(username)
    }

    private fun validateUser(username: Username): UserAuth {
        val userAuth = authDao.getUserAuth(username) ?: throw UnknownUserException(username)

        authDao.getUserSuspension(userAuth.userId)
                ?.let { it.end != null }
                ?.let { throw UserSuspendedException(userAuth.userId, temporary = it) }

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
                userAuth.username.bytes,
                userAuth.salt,
                A,
                B,
                K)

        // Password match fail :(
        if (M1 != M1s) {
            logger.info { "Password mismatch for ${userAuth.username}" }
            handleAuthFailure(userAuth.userId)

            return null
        }

        // Ok! Update session key
        authDao.updateUserSessionKey(userAuth.userId, K)

        // and return M2
        return M2ByteArray(sha1.digest(
                A.bytes,
                M1.bytes,
                K.bytes))
    }

    fun challengeReconnect(username: Username): ReconnectChallenge {
        val sessionKey = authDao.getUserAuth(username)
                ?.let { authDao.getUserSessionKey(it.userId) }
                ?: throw UnknownUserException(username)

        return ReconnectChallenge(
                username = username,
                sessionKey = sessionKey,
                reconnectProof = ByteArrays.randomBytes(16))
    }

    fun proofReconnect(challenge: ReconnectChallenge, R1: BigUnsignedInteger, R2: BigUnsignedInteger): UserId? {
        val sha = sha1.digest(
                challenge.username.bytes,
                R1.bytes,
                challenge.reconnectProof,
                challenge.sessionKey.bytes)

        return if (sha contentEquals R2.bytes) authDao.getUserAuth(challenge.username)?.userId
        else null
    }

    private fun handleAuthFailure(userId: UserId) {
        if (authDao.recordUserAuthFailure(userId) > SUSPEND_AFTER_LOGIN_FAILURES) {
            logger.info { "Suspending $userId because > 5 authentication failures" }
            authDao.suspendUser(userId, now(), now() + LOGIN_FAILURE_SUSPENSION_DURATION)
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

data class ReconnectChallenge(
        val username: Username,
        val sessionKey: BigUnsignedInteger,
        val reconnectProof: ByteArray)

class M2ByteArray(bytes: ByteArray): ComparableByteArray(bytes)

sealed class SuspendedException(message: String, val temporary: Boolean): RuntimeException("$message (temporary=$temporary)")
class IpBannedException(ip: InetAddress, temporary: Boolean): SuspendedException("IP banned: $ip", temporary)
class UserSuspendedException(username: UserId, temporary: Boolean): SuspendedException("User suspended: $username", temporary)
class UnknownUserException(userId: Any): RuntimeException("Unknown user: $userId")