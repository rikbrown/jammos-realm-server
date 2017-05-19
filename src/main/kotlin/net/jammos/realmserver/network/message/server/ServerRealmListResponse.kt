package net.jammos.realmserver.network.message.server

import mu.KLogging
import net.jammos.realmserver.network.AuthCommand
import net.jammos.realmserver.realm.Realm
import net.jammos.realmserver.realm.RealmFlag
import java.io.ByteArrayOutputStream
import java.io.DataOutput
import java.io.DataOutputStream

data class ServerRealmListResponse(
        val realmCount: Map<Realm, Int>
): ServerAuthMessage {

    companion object : KLogging()

    override fun write(output: DataOutput) {
        logger.debug("Writing realm list message")

        // create realm buffer
        val baos = ByteArrayOutputStream()
        val byteBuffer = DataOutputStream(baos)

        byteBuffer.writeInt(0) // unused
        byteBuffer.write(realmCount.size) // n realms

        // write realms (realm + char count)
        realmCount.forEach { t, u -> RealmListResponseRealm(t, u).write(byteBuffer) }
        byteBuffer.writeShort(2) // '2' end marker

        // ---

        // begin actual writing
        AuthCommand.REALM_LIST.write(output) // command

        // realm buffer size (force little endian)
        val realmBufferSize: ByteArray = byteArrayOf(
            (baos.size() and 0xff).toByte(),
            (baos.size() shr 8 and 0xff).toByte())

        output.write(realmBufferSize)

        // realm buffer
        output.write(baos.toByteArray()) // and buffer

        logger.debug("Done writing realm list message")
    }

    private data class RealmListResponseRealm(
            val realm: Realm,
            val numberOfChars: Int
    ) {
        fun write(output: DataOutput) {
            output.write(realm.realmType.value.bytes(4))

            // realm flags
            var realmFlag = RealmFlag.REALM_FLAG_NONE.value
            realm.realmFlags.forEach { realmFlag = realmFlag or it.value }

            output.write(realmFlag)
            output.write(realm.name.toByteArray(Charsets.UTF_8)) // name
            output.write(0) // end name marker (I think)

            output.write(realm.address.toByteArray(Charsets.UTF_8))
            output.write(0) // end address marker (I think)

            // (playerCount / maxPlayerCount * 2)
            output.writeFloat(realm.playerCount.toFloat() / realm.maxPlayerCount.toFloat() * 2)

            //output.writeFloat(((1000/1000)* 2.toFloat())) // pop level (tbc)

            output.write(numberOfChars) // number of chars

            output.write(1) // realm category (? using timezone on mangos)
            output.write(0) // unknown
        }
    }


}
