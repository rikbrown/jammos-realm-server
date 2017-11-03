package net.jammos.realmserver.network

import io.netty.util.AttributeKey
import net.jammos.realmserver.auth.AuthChallenge
import net.jammos.realmserver.auth.ReconnectChallenge
import net.jammos.utils.auth.UserId

object JammosRealmAttributes {
    val AUTH_CHALLENGE_ATTRIBUTE = AttributeKey.valueOf<AuthChallenge>("authChallenge")!!
    val USERID_ATTRIBUTE = AttributeKey.valueOf<UserId>("userId")!!
    val RECONNECT_CHALLENGE_ATTRIBUTE = AttributeKey.valueOf<ReconnectChallenge>("reconnectChallenge")!!
}