package net.jammos.realmserver.data

import net.jammos.realmserver.auth.AuthCommand
import net.jammos.realmserver.realms.Realm
import net.jammos.realmserver.realms.RealmFlag
import java.io.ByteArrayOutputStream
import java.io.DataOutput
import java.io.DataOutputStream
import kotlin.text.Charsets.UTF_8

data class RealmListResponse(
        val realmCount: Map<Realm, Int>
) {


    fun write(output: DataOutput) {
        // create realm buffer
        val baos = ByteArrayOutputStream()
        val byteBuffer = DataOutputStream(baos)

        byteBuffer.writeInt(0) // unused
        byteBuffer.write(realmCount.size) // n realms

        // write realms (realm + char count)
        realmCount.forEach { t, u -> RealmListResponseRealm(t, u).write(byteBuffer) }
        byteBuffer.writeShort(2) // '2' end marker

        // begin actual writing
        AuthCommand.REALM_LIST.write(output) // command
        output.writeShort(baos.size()) // realm buffer size
        output.write(baos.toByteArray()) // and buffer
    }
}

data class RealmListResponseRealm(
        val realm: Realm,
        val numberOfChars: Int
) {
    fun write(output: DataOutput) {
        output.write(realm.realmType.value.bytes(4))

        // realm flags
        var realmFlag = RealmFlag.REALM_FLAG_NONE.value
        realm.realmFlags.forEach { realmFlag = realmFlag or it.value }

        output.write(realmFlag)
        output.write(realm.name.toByteArray(UTF_8)) // name
        output.write(0) // end name marker (I think)

        output.write(realm.address.toByteArray(UTF_8))
        output.write(0) // end address marker (I think)

        output.writeFloat(0.001F) // how does pop level even work

        //output.writeFloat(((1000/1000)* 2.toFloat())) // pop level (tbc)

        output.write(numberOfChars) // number of chars

        output.write(1) // realm category (? using timezone on mangos)
        output.write(0) // unknown
    }
}