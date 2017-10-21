package net.jammos.realmserver.auth

import net.jammos.realmserver.auth.crypto.CryptoManager
import net.jammos.utils.ByteArrays.randomBytes
import net.jammos.utils.types.BigUnsignedInteger
import java.net.InetAddress
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

interface AuthDao {
    fun getUserAuth(username: Username): UserAuth?
    fun getUserSuspension(username: Username): UserSuspension?

    fun createUser(username: Username, password: String): UserAuth

    fun suspendUser(username: Username, start: Instant, end: Instant? = null)

    fun updateUserSessionKey(username: Username, sessionKey: BigUnsignedInteger)
    fun recordUserAuthFailure(username: Username): Long

    // TODO: somewhere else?
    fun suspendIp(ip: InetAddress, end: Instant?)
    fun getIpSuspension(ip: InetAddress): IpSuspension?

}

data class IpSuspension(val end: Instant?)

class InMemoryAuthDao(private val cryptoManager: CryptoManager): AuthDao {
    val users = ConcurrentHashMap<Username, UserAuth>()
    val suspensions = ConcurrentHashMap<Username, UserSuspension>()
    val ipBans = ConcurrentHashMap<InetAddress, IpSuspension>()

    override fun createUser(username: Username, password: String): UserAuth {

        val salt = SaltByteArray(randomBytes(32))
        val passwordUpper = password.toUpperCase()

        val loginHash = cryptoManager.createPrivateKey(
                username.toByteArray(UTF_8),
                passwordUpper.toByteArray(UTF_8),
                salt)

        val user = UserAuth(
                username = username,
                salt = salt,
                verifier = cryptoManager.createUserVerifier(loginHash))
        users[username] = user

        return user
    }

    override fun getUserAuth(username: Username) = users[username]

    override fun suspendUser(username: Username, start: Instant, end: Instant?) {
        suspensions[username] = UserSuspension(
                    start = start,
                    end = end)
    }

    override fun getUserSuspension(username: Username) = suspensions[username]

    override fun updateUserSessionKey(username: Username, sessionKey: BigUnsignedInteger) {

    }

    override fun recordUserAuthFailure(username: Username): Long {
        return 0
    }

    override fun suspendIp(ip: InetAddress, end: Instant?) {
        ipBans[ip] = IpSuspension(end)
    }

    override fun getIpSuspension(ip: InetAddress) = ipBans[ip]

}