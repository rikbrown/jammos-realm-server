package net.jammos.realmserver.auth

import net.jammos.realmserver.auth.crypto.CryptoManager
import net.jammos.realmserver.utils.ByteArrays.randomBytes
import java.net.InetAddress
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

interface AuthDao {
    fun createUser(username: Username, password: String): User
    fun getUser(username: Username): User?
    fun suspendUser(user: User, start: Instant, end: Instant?): User?
    fun recordUserLogon(user: User, ip: InetAddress, at: Instant, successful: Boolean = true)

    // TODO: somewhere else?
    fun suspendIp(ip: InetAddress, end: Instant?)
    fun getIpSuspension(ip: InetAddress): IpSuspension?

}

data class IpSuspension(val end: Instant?)

class InMemoryAuthDao(private val cryptoManager: CryptoManager): AuthDao {

    val users = ConcurrentHashMap<Username, User>()
    val ipBans = ConcurrentHashMap<InetAddress, IpSuspension>()

    override fun createUser(username: Username, password: String): User {

        val salt = randomBytes(32)
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

    override fun suspendUser(user: User, start: Instant, end: Instant?): User? {
        return users[user.username]
            ?.copy(
                suspension = UserSuspension(
                    start = start,
                    end = end))
            ?.let {
                users[user.username] = it
                it
            }
    }

    override fun recordUserLogon(user: User, ip: InetAddress, at: Instant, successful: Boolean) {
        // TODO: implement this
    }

    override fun suspendIp(ip: InetAddress, end: Instant?) {
        ipBans[ip] = IpSuspension(end)
    }

    override fun getIpSuspension(ip: InetAddress) = ipBans[ip]

}