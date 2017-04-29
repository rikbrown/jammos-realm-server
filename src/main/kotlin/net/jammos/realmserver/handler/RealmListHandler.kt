package net.jammos.realmserver.handler

import com.google.common.base.Preconditions.checkArgument
import net.jammos.realmserver.auth.AuthCommand
import net.jammos.realmserver.auth.AuthDao
import net.jammos.realmserver.auth.Username
import net.jammos.realmserver.data.RealmListResponse
import net.jammos.realmserver.realms.RealmDao
import java.io.DataInput
import java.io.DataOutput

class RealmListHandler(
        override val input: DataInput,
        val output: DataOutput,
        val username: Username,

        val authDao: AuthDao,
        val realmDao: RealmDao
): Handler {

    override fun handle(): Handler? {
        requireCommand(AuthCommand.REALM_LIST)

        // get user
        val user = authDao.getUser(username)
        checkArgument(user != null, "user tried to login but doesn't exist")
        checkArgument(!user!!.isBanned, "user tried to login but is banned")

        input.skipBytes(4) // skip 4 zero bytes

        // get realms
        val realms = realmDao.listRealms()
        val realmCount = realms
                .map { realm -> Pair(realm, realmDao.getCharacterCount(realm.id, username)) }
                .toMap()

        RealmListResponse(realmCount).write(output)

        return this
    }

}