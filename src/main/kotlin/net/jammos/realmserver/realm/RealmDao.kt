package net.jammos.realmserver.realm

import net.jammos.utils.auth.Username
import net.jammos.utils.auth.Username.Username.username
import net.jammos.utils.types.InternetAddress
import net.jammos.utils.extensions.get

interface RealmDao {
    fun listRealms(): Set<Realm>
    fun getCharacterCount(realmId: RealmId, username: Username): Int
}

class InMemoryRealmDao: RealmDao {
    val realms = setOf(
            Realm(
                    id = RealmId("test1"),
                    name = "Rank 11 Druids Only",
                    address = InternetAddress("127.0.0.1", 1234),
                    realmType = RealmType.PVP),
            Realm(
                    id = RealmId("test2"),
                    name = "Synergy (Golden Gods)",
                    address = InternetAddress("127.0.0.1", 1234),
                    realmType = RealmType.RPPVP),
            Realm(
                    id = RealmId("test3"),
                    name = "Joe plz come here",
                    address = InternetAddress("127.0.0.1", 1234),
                    realmFlags = setOf(RealmFlag.REALM_FLAG_NEW_PLAYERS)),
            Realm(
                    id = RealmId("test4"),
                    name = "RIP Feenix",
                    address = InternetAddress("127.0.0.1", 1234),
                    realmFlags = setOf(RealmFlag.REALM_FLAG_OFFLINE))
    )

    val realmCharacterCount = mapOf(
            username("rikbrown") to mapOf(
                    RealmId("test1") to 1,
                    RealmId("test2") to 2))

    override fun listRealms(): Set<Realm> {
        return realms
    }

    override fun getCharacterCount(realmId: RealmId, username: Username): Int {
        return realmCharacterCount[username][realmId] ?: 0
    }
}