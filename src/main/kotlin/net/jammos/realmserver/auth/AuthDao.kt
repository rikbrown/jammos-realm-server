package net.jammos.realmserver.auth

import net.jammos.realmserver.auth.crypto.CryptoManager
import net.jammos.realmserver.utils.ByteArrays.randomBytes
import net.jammos.realmserver.utils.types.BigUnsignedInteger
import java.net.InetAddress
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

interface AuthDao {
    fun createUser(username: Username, password: String): User
    fun getUser(username: Username): User?
    fun suspendUser(username: Username, start: Instant, end: Instant? = null): User?
    fun updateUserSessionKey(username: Username, sessionKey: BigUnsignedInteger)
    fun recordUserAuthFailure(username: Username): Long

    // TODO: somewhere else?
    fun suspendIp(ip: InetAddress, end: Instant?)
    fun getIpSuspension(ip: InetAddress): IpSuspension?

}

data class IpSuspension(val end: Instant?)

class InMemoryAuthDao(private val cryptoManager: CryptoManager): AuthDao {
    val users = ConcurrentHashMap<Username, User>()
    val ipBans = ConcurrentHashMap<InetAddress, IpSuspension>()

    override fun createUser(username: Username, password: String): User {

        val salt = SaltByteArray(randomBytes(32))
        val passwordUpper = password.toUpperCase()

        val loginHash = cryptoManager.createPrivateKey(
                username.toByteArray(UTF_8),
                passwordUpper.toByteArray(UTF_8),
                salt)

        val user = User(
                username = username,
                salt = salt,
                verifier = cryptoManager.createUserVerifier(loginHash))
        users[username] = user

        return user
    }

    override fun getUser(username: Username): User? = users[username]

    override fun suspendUser(username: Username, start: Instant, end: Instant?): User? {
        return users[username]
            ?.copy(
                suspension = UserSuspension(
                    start = start,
                    end = end))
            ?.apply { users[username] = this }
    }

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