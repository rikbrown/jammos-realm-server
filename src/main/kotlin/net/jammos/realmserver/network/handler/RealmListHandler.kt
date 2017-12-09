package net.jammos.realmserver.network.handler

import io.netty.channel.ChannelHandlerContext
import mu.KLogging
import net.jammos.realmserver.auth.AuthManager
import net.jammos.realmserver.network.JammosRealmAttributes.USERID_ATTRIBUTE
import net.jammos.realmserver.network.message.client.ClientRealmListMessage
import net.jammos.realmserver.network.message.server.ServerRealmListResponse
import net.jammos.utils.realm.RealmDao
import net.jammos.utils.network.handler.JammosHandler

class RealmListHandler(
        private val authManager: AuthManager,
        private val realmDao: RealmDao): JammosHandler() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg !is ClientRealmListMessage) return pass(ctx, msg)
        val userId = attr(ctx, USERID_ATTRIBUTE)
            ?: throw IllegalStateException("Cannot handle realm list without authenticated username")

        // validate user still exists and is not suspended at this point
        authManager.validateUser(userId)

        // get realms
        val realms = realmDao.listRealms()
        val realmCount = realms
                .map { realm -> realm to realmDao.getUserCharacterCount(realm.id, userId) }
                .toMap()

        // respond
        logger.debug { "Responding with realm list"}
        respond(ctx, ServerRealmListResponse(realmCount))
    }

    companion object : KLogging()
}

