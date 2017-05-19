package net.jammos.realmserver.auth

import net.jammos.realmserver.auth.crypto.CryptoManager
import net.jammos.realmserver.utils.ByteArrays.randomBytes
import java.net.InetAddress
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Clock
import java.time.Instant
import java.time.Instant.now
import java.util.Collections.synchronizedSet
import java.util.concurrent.ConcurrentHashMap

interface AuthDao {
    fun createUser(username: Username, password: String): User
    fun getUser(username: Username): User?
    fun suspendUser(user: User, end: Instant?): User?

    // TODO: somewhere else?
    fun banIp(ip: InetAddress)
    fun isIpBanned(ip: InetAddress): Boolean

}

class InMemoryAuthDao(
        private val clock: Clock = Clock.systemUTC(),
        private val cryptoManager: CryptoManager): AuthDao {
    val users = ConcurrentHashMap<Username, User>()
    val bannedIps: MutableSet<InetAddress> = synchronizedSet(HashSet<InetAddress>())

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

    override fun suspendUser(user: User, end: Instant?): User? {
        return users[user.username]?.copy(
                suspension = UserSuspension(
                        start = now(clock),
                        end = end))
            ?.let {
                users[user.username] = it
                return it
            }
    }

    override fun banIp(ip: InetAddress) {
        bannedIps += ip
    }

    override fun isIpBanned(ip: InetAddress) = bannedIps.contains(ip)

}