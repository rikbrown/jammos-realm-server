package net.jammos.realmserver.realms

import net.jammos.realmserver.utils.types.InternetAddress
import java.util.*

data class Realm(
        val id: RealmId,
        val name: String,
        val address: InternetAddress,
        val realmFlags: Set<RealmFlag> = setOf(),
        val realmType: RealmType = RealmType.NORMAL,
        val playerCount: Int = 1,
        val maxPlayerCount: Int = 1000)

data class RealmId(val realmId: String) {
    companion object {
        fun generate(): RealmId {
            return RealmId(UUID.randomUUID().toString())
        }
    }
}