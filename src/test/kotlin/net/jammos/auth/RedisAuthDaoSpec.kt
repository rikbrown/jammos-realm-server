package net.jammos.auth

import com.lambdaworks.redis.RedisClient
import net.jammos.realmserver.auth.RedisAuthDao
import net.jammos.realmserver.auth.Username
import net.jammos.realmserver.auth.crypto.CryptoManager
import net.jammos.utils.types.BigUnsignedInteger
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not be`
import org.apache.commons.text.RandomStringGenerator
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Instant

private val stringGenerator = RandomStringGenerator.Builder()
    .withinRange('a'.toInt(), 'z'.toInt())
    .build()

object RedisAuthDaoSpec: Spek({
    /*val redis by resource {
        val provider = RedisExecProvider.build()
                .override(OS.MAC_OS_X, Architecture.x86_64, "/usr/local/bin/redis-server")
        CloseableRedis(RedisServer(provider, 4321))
    }*/
    val testPrefix = "_TEST:${stringGenerator.generate(5).toUpperCase()}"
    val redisClient by memoized { RedisClient.create("redis://localhost") }
    val redisConn by memoized { redisClient.connect().sync() }
    val authDao by memoized { RedisAuthDao(redisClient, CryptoManager()) }
    fun newUsername() = Username.username("$testPrefix:${stringGenerator.generate(5)}")

    afterGroup {
        redisConn.keys("*$testPrefix:*").forEach { redisConn.del(it) }
    }

    given("no existing user") {
        on("createUser") {
            val username = newUsername()
            val user = authDao.createUser(username, "rank11")

            it("should return a new user") {
                user.username `should equal` username
            }

            it("should create a user entry") {
                val gotUser = authDao.getUserAuth(username)
                gotUser `should equal` user
            }
        }
    }

    given("an existing user") {
        val username = newUsername()
        beforeGroup { authDao.createUser(username, "rank11") }

        on("getUserAuth") {
            val userAuth = authDao.getUserAuth(username)!!

            it("should return the given user auth") {
                userAuth.username `should equal` username
            }
        }

        on("updateUserSessionKey") {
            val K = BigUnsignedInteger(123)
            authDao.updateUserSessionKey(username, K)

            it("should update") {
                redisConn.get("user:$username:session_key") `should equal` K.toString()
            }
        }

        on("recordUserAuthFailure") {
            val failure1 = authDao.recordUserAuthFailure(username)
            val failure2 = authDao.recordUserAuthFailure(username)

            it("should return an incrementing failure account") {
                failure1 `should equal` 1L
                failure2 `should equal` 2L
            }

            it("should increment") {
                redisConn.get("user:$username:auth_failure_count") `should equal` "2"
            }
        }
    }

    given("an existing user who has been bad") {
        val now = Instant.parse("2017-07-01T12:34:56Z")
        val username = newUsername()
        beforeGroup { authDao.createUser(username, "rank11") }

        on("suspendUser forever") {
            authDao.suspendUser(username, start = now)

            it("should suspend the user") {
                val gotSuspension = authDao.getUserSuspension(username)
                gotSuspension `should not be` null
                gotSuspension!!.start `should equal` now
                gotSuspension.end `should be` null
            }
        }

        on("suspendUser temporarily") {
            val end = now.plusSeconds(60)
            authDao.suspendUser(username, start = now, end = end)

            it("should suspend the user with an end") {
                val gotSuspension = authDao.getUserSuspension(username)
                gotSuspension `should not be` null
                gotSuspension!!.start `should equal` now
                gotSuspension.end `should equal` end
            }

        }
    }

    given("unknown user") {
        val username = newUsername()

        on("getUserAuth") {
            val user = authDao.getUserAuth(username)

            it("should return nothing") {
                user `should be` null
            }
        }

    }

})