package net.jammos.realmserver.realm

import net.jammos.realmserver.utils.types.BigUnsignedInteger

enum class RealmType(val value: BigUnsignedInteger) {
    NORMAL(BigUnsignedInteger(0)),
    PVP(BigUnsignedInteger(1)),
    RP(BigUnsignedInteger(6)),
    RPPVP(BigUnsignedInteger(8))
}

