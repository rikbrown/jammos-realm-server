package net.jammos.realmserver.network.message.server

import com.google.common.base.Preconditions
import io.netty.buffer.ByteBuf
import net.jammos.realmserver.network.AuthCommand
import net.jammos.realmserver.network.AuthResult
import net.jammos.utils.auth.SaltByteArray
import net.jammos.utils.extensions.writeByte
import net.jammos.utils.types.BigUnsignedInteger


data class ServerAuthLogonChallengeResponse(
        val status: AuthResult,
        val successData: SuccessData? = null
): ServerAuthMessage {
    override fun write(output: ByteBuf) {
        output.writeByte(AuthCommand.LOGON_CHALLENGE)
        output.writeByte(0x00) // error byte
        output.writeByte(status)
        successData?.write(output)
    }

    data class SuccessData(
            val g: BigUnsignedInteger,
            val N: BigUnsignedInteger,

            val B: BigUnsignedInteger,
            val s: SaltByteArray,
            val unk3: ByteArray,
            val securityFlags: Int
    ) {
        fun write(output: ByteBuf) {

            // write B, padded to 32
            output.writeBytes(B.bytes(32))

            // write g
            output.writeByte(1) // length (1 byte)
            output.writeBytes(g.bytes)

            // write N, padded to 32
            output.writeByte(32) // length (32 bytes)
            output.writeBytes(N.bytes)

            // write s, size 32
            output.writeBytes(s.bytes)

            // write unk3, size 16
            output.writeBytes(unk3)

            // write security flags
            Preconditions.checkArgument(securityFlags == 0) // only "0" supported for now
            output.writeByte(securityFlags)
        }
    }
}