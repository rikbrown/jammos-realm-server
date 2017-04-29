package net.jammos.realmserver.data

import com.google.common.base.Preconditions.checkArgument
import net.jammos.realmserver.auth.AuthCommand
import net.jammos.realmserver.auth.AuthResult
import net.jammos.realmserver.utils.types.BigUnsignedInteger
import java.io.DataOutput

data class AuthLogonChallengeResponse(
        val status: AuthResult,
        val successData: AuthLogonChallengeResponseSuccessData? = null
) {
    fun write(output: DataOutput) {
        AuthCommand.LOGON_CHALLENGE.write(output)
        output.writeByte(0x00) // error byte
        status.write(output) // status byte

        successData?.write(output)
    }
}

data class AuthLogonChallengeResponseSuccessData(
        val g: BigUnsignedInteger,
        val N: BigUnsignedInteger,

        val B: BigUnsignedInteger,
        val s: ByteArray,
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
        output.write(s)

        // write unk3, padded to 16
        output.write(unk3)

        // write security flags
        checkArgument(securityFlags == 0) // only "0" supported for now
        output.writeByte(securityFlags)
    }
}
