package net.jammos.realmserver.network.message.server

import com.google.common.base.Preconditions
import net.jammos.realmserver.auth.M2ByteArray
import net.jammos.realmserver.network.AuthCommand
import net.jammos.realmserver.network.AuthResult
import java.io.DataOutput

data class ServerAuthLogonProofResponse(
        val status: AuthResult,
        val successData: SuccessData? = null
): ServerAuthMessage {

    override fun write(output: DataOutput) {
        AuthCommand.LOGON_PROOF.write(output)
        status.write(output)
        successData?.write(output)
    }

    data class SuccessData(
            val M2: M2ByteArray,
            val unk2: Int = 0) {

        init {
            Preconditions.checkArgument(M2.m2.size == 20)
        }

        fun write(output: DataOutput) {
            output.write(M2.m2)
            output.writeInt(unk2)
        }
    }

}

