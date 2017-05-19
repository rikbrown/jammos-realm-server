package net.jammos.realmserver.realm

enum class RealmFlag(val value: Int) {
    REALM_FLAG_NONE(0x00),
    REALM_FLAG_OFFLINE(0x02),
    REALM_FLAG_NEW_PLAYERS(0x40);
}

