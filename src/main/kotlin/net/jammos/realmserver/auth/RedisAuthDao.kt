package net.jammos.realmserver.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.common.base.Strings
import com.lambdaworks.redis.RedisClient
import mu.KLogging
import net.jammos.realmserver.auth.crypto.CryptoManager
import net.jammos.realmserver.utils.ByteArrays.randomBytes
import net.jammos.realmserver.utils.types.BigUnsignedInteger
import java.net.InetAddress
import java.nio.charset.StandardCharsets.UTF_8
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException

class RedisAuthDao(
        redisClient: RedisClient,
        private val cryptoManager: CryptoManager) : AuthDao {

    private companion object: KLogging() {
        val AUTH_FAILURES_TTL: Duration = Duration.ofMinutes(30)

        val objectMapper: ObjectMapper = ObjectMapper()
                .registerKotlinModule()
                .registerModule(JavaTimeModule())

        fun json(obj: Any): String = objectMapper.writeValueAsString(obj)
        inline fun <reified T: Any> fromJson(json: String): T = objectMapper.readValue(json)

        fun userAuthKey(username: Username) = "user:$username:auth"
        fun userSuspensionKey(username: Username) = "user:$username:suspension"
        fun userSessionKeyKey(username: Username) = "user:$username:session_key"
        fun userAuthFailuresKey(username: Username) = "user:$username:auth_failure_count"
        fun ipSuspensionKey(ip: InetAddress) = "ip:$ip:suspended_until"

        @Suppress("ArrayInDataClass") // just using for serialisation, don't need to compare
        data class RedisUserAuth(
                val salt: ByteArray,
                val verifier: ByteArray)
        data class RedisUserSuspension(
                val start: Instant,
                val end: Instant?) {
            fun fromRedis() = UserSuspension(start = start, end = end)
        }
        fun UserAuth.toRedis(): RedisUserAuth = RedisUserAuth(salt = salt.bytes, verifier = verifier.bytes)
        fun UserSuspension.toRedis(): RedisUserSuspension = RedisUserSuspension(start = start, end = end)
    }

    private val conn = redisClient.connect().sync()

    override fun getUserAuth(username: Username): UserAuth? {
        return conn.get(userAuthKey(username))
                ?.let { fromJson<RedisUserAuth>(it) }
                ?.let { (salt, verifier) ->
                    UserAuth(
                            username = username,
                            salt = SaltByteArray(salt),
                            verifier = BigUnsignedInteger(verifier))
                }
    }

    override fun getUserSuspension(username: Username): UserSuspension? {
        return conn.get(userSuspensionKey(username))
                ?.let { fromJson<RedisUserSuspension>(it) }
                ?.fromRedis()
    }

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

        // save in redis
        conn.set(userAuthKey(username), json(user.toRedis()))

        return user
    }

    override fun suspendUser(username: Username, start: Instant, end: Instant?) {
        conn.set(userSuspensionKey(username), json(UserSuspension(
                start = start,
                end = end).toRedis()))
    }

    override fun updateUserSessionKey(username: Username, sessionKey: BigUnsignedInteger) {
        conn.set(userSessionKeyKey(username), sessionKey.toString())
    }

    override fun recordUserAuthFailure(username: Username): Long {
        val key = userAuthFailuresKey(username)
        return conn.incr(key)
                // expire the auth failures after TTL
                ?.apply { conn.expire(key, AUTH_FAILURES_TTL.seconds) }
                ?: 0
    }

    override fun suspendIp(ip: InetAddress, end: Instant?) {
        val key = ipSuspensionKey(ip)

        // set suspension
        conn.set(key, end?.toString() ?: "")

        // expire suspension key when the suspension is over, if it has an end
        end?.apply { conn.expireat(key, epochSecond) }
    }

    override fun getIpSuspension(ip: InetAddress): IpSuspension? {
        return try {
            conn.get(ipSuspensionKey(ip))
                    // empty string means forever
                    ?.let { IpSuspension(Strings.emptyToNull(it) ?.let(Instant::parse)) }

        } catch (e: DateTimeParseException) {
            logger.error(e) { "Failed to parse expiry date" }
            return null
        }
    }
}

/*
    user:<username>:auth = {
        password_sha = <binary>,
        salt: <binary>,
        verifier: <binary>,
    }

    user:<username>:session_key = <binary>
    user:<username>:auth_failure_count = <int>


    user:<username>:suspension: {
        start: <time>,
        end: <time>
    }

    io:<ip>:suspended_until: <time>
 */