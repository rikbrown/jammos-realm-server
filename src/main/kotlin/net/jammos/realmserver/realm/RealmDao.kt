package net.jammos.realmserver.realm

import net.jammos.utils.auth.Username

interface RealmDao {
    fun updateRealm(realm: Realm)
    fun listRealms(): Set<Realm>

    fun getUserCharacterCount(realmId: RealmId, username: Username): Int
    fun setUserCharacterCount(realmId: RealmId, username: Username, count: Int)
}
