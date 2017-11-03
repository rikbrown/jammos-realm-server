package net.jammos.realmserver.network.message.server

import com.google.common.base.Preconditions
import io.netty.buffer.ByteBuf
import net.jammos.realmserver.auth.M2ByteArray
import net.jammos.realmserver.network.AuthCommand
import net.jammos.realmserver.network.AuthResult
import net.jammos.utils.extensions.writeByte

data class ServerAuthLogonProofResponse(
        val status: AuthResult,
        val successData: SuccessData? = null
): ServerAuthMessage {

    override fun write(output: ByteBuf) {
        output.writeByte(AuthCommand.LOGON_PROOF)
        output.writeByte(status)
        successData?.write(output)
    }

    data class SuccessData(
            val M2: M2ByteArray,
            val unk2: Int = 0) {

        init {
            Preconditions.checkArgument(M2.bytes.size == 20)
        }

        fun write(output: ByteBuf) {
            output.writeBytes(M2.bytes)
            output.writeInt(unk2)
        }
    }

}

