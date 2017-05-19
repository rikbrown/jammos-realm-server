package net.jammos.realmserver.network.step

import com.google.common.base.Preconditions.checkArgument
import mu.KLogging
import net.jammos.realmserver.auth.AuthManager
import net.jammos.realmserver.auth.Username
import net.jammos.realmserver.network.message.client.ClientRealmListMessage
import net.jammos.realmserver.network.message.server.ServerRealmListResponse
import net.jammos.realmserver.realm.RealmDao

class RealmListStep(
        val username: Username,

        val authManager: AuthManager,
        val realmDao: RealmDao) : Step<ClientRealmListMessage, ServerRealmListResponse>(ClientRealmListMessage::class) {

    companion object : KLogging()

    override fun handle0(msg: ClientRealmListMessage): ResponseAndNextStep<ServerRealmListResponse> {
        logger.debug { "Handling realm list for apparent user($username)" }

        // validate user (happened at auth, but things change)
        // TODO: exception or can we return the auth error like challenge? prob not
        val user = authManager.getUser(username) ?: throw IllegalArgumentException("user($username) tried to logon but does not exist")
        checkArgument(user.suspension != null, "user($username) tried to logon but is suspended")

        // get realms
        val realms = realmDao.listRealms()
        val realmCount = realms
                .map { realm -> realm to realmDao.getCharacterCount(realm.id, username) }
                .toMap()

        // respond
        logger.debug { "Responding with realm list"}
        return ResponseAndNextStep(
                // list realms
                response = ServerRealmListResponse(realmCount),
                // we keep listening :))
                nextStep = this
        )
    }


}