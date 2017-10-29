package net.jammos.realmserver.realm

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.lambdaworks.redis.RedisClient
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.runBlocking
import net.jammos.utils.auth.Username
import net.jammos.utils.types.InternetAddress

private const val REALMS_KEY = "realms"
private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()
private fun json(obj: Any): String = objectMapper.writeValueAsString(obj)
private inline fun <reified T: Any> fromJson(json: String): T = objectMapper.readValue(json)

private fun realmPlayerCountKey(realmId: RealmId) = "realm:$realmId:playerCount"
private fun realmUserCharacterCountKey(realmId: RealmId, username: Username) = "realm:$realmId:user:$username:characterCount"

private data class RedisRealm(
        val name: String,
        val address: InternetAddress,
        val type: String,
        val flags: Set<String>,
        val maxPlayerCount: Int,
        val playerCount: Int = 0) {

    fun fromRedis(realmId: RealmId) = Realm(
            id = realmId,
            name = name,
            address = address,
            realmFlags = flags.map { RealmFlag.valueOf(it) }.toSet(),
            realmType = RealmType.valueOf(type),
            maxPlayerCount = maxPlayerCount)
}
private fun Realm.toRedis() = RedisRealm(
        name = name,
        address = address,
        type = realmType.name,
        flags = realmFlags.map { it.name }.toSet(),
        maxPlayerCount = maxPlayerCount)

class RedisRealmDao(redisClient: RedisClient): RealmDao {
    private val conn = redisClient.connect().sync()

    override fun listRealms(): Set<Realm> {
        return runBlocking { conn.hgetall(REALMS_KEY)
                .mapKeys { (realmId, _) -> RealmId(realmId) }
                .mapValues { (_, v) -> fromJson<RedisRealm>(v) }
                .mapValues { (realmId, realm) -> async { realm.copy(playerCount = getPlayerCount(realmId)) }}
                .mapValues { (_, d) ->  d.await() }
                .map { (realmId, realm) -> realm.fromRedis(realmId) }
                .toSet()
        }
    }

    override fun updateRealm(realm: Realm) {
        conn.hset(REALMS_KEY, realm.id.toString(), json(realm.toRedis()))
    }

    override fun getUserCharacterCount(realmId: RealmId, username: Username): Int {
        return conn.get(realmUserCharacterCountKey(realmId, username))?.toInt() ?: 0
    }

    override fun setUserCharacterCount(realmId: RealmId, username: Username, count: Int) {
        conn.set(realmUserCharacterCountKey(realmId, username), count.toString())
    }

    private suspend fun getPlayerCount(realmId: RealmId) = conn.get(realmPlayerCountKey(realmId))?.toInt() ?: 0
}
/*

realms = hash {
    test1 = string {
        name: "foo",
        address: "foo",
        type: "pvp",
        flags: [...]
    }
}

realm:realmId:playerCount = int

realm:$realmId:user:$userId:characterCounts = hash {
    test1 = 1
}

 */