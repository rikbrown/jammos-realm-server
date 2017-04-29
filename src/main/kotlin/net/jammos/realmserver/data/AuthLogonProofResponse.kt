package net.jammos.realmserver.data

import com.google.common.base.Preconditions.checkArgument
import net.jammos.realmserver.auth.AuthCommand
import net.jammos.realmserver.auth.AuthResult
import java.io.DataOutput

data class AuthLogonProofResponse(
        val status: AuthResult,
        val successData: AuthLogonProofResponseSuccessData? = null
) {

    fun write(output: DataOutput) {
        AuthCommand.LOGON_PROOF.write(output)
        status.write(output)
        successData?.write(output)

    }
}

data class AuthLogonProofResponseSuccessData(
        val M2: ByteArray,
        val unk2: Int = 0) {

    init {
        checkArgument(M2.size == 20)
    }

    fun write(output: DataOutput) {
        output.write(M2)
        output.writeInt(unk2)
    }
}