package net.jammos.realmserver.realm

import net.jammos.utils.auth.UserId

interface RealmDao {
    fun updateRealm(realm: Realm)
    fun listRealms(): Set<Realm>

    fun getUserCharacterCount(realmId: RealmId, userId: UserId): Int
    fun setUserCharacterCount(realmId: RealmId, userId: UserId, count: Int)
}
