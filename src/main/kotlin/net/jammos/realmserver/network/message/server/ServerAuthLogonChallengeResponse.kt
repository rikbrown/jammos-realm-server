package net.jammos.realmserver.network.message.server

import com.google.common.base.Preconditions
import net.jammos.realmserver.network.AuthCommand
import net.jammos.realmserver.network.AuthResult
import net.jammos.utils.auth.SaltByteArray
import net.jammos.utils.types.BigUnsignedInteger
import java.io.DataOutput


data class ServerAuthLogonChallengeResponse(
        val status: AuthResult,
        val successData: SuccessData? = null
): ServerAuthMessage {
    override fun write(output: DataOutput) {
        AuthCommand.LOGON_CHALLENGE.write(output)
        output.writeByte(0x00) // error byte
        status.write(output) // status byte

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
        fun write(output: DataOutput) {

            // write B, padded to 32
            output.write(B.bytes(32))

            // write g
            output.writeByte(1) // length (1 byte)
            output.write(g.bytes)

            // write N, padded to 32
            output.writeByte(32) // length (32 bytes)
            output.write(N.bytes)

            // write s, size 32
            output.write(s.bytes)

            // write unk3, size 16
            output.write(unk3)

            // write security flags
            Preconditions.checkArgument(securityFlags == 0) // only "0" supported for now
            output.writeByte(securityFlags)
        }
    }
}