package net.jammos.realmserver.auth

import net.jammos.realmserver.utils.types.WriteableByte
import java.io.DataInput

enum class AuthCommand(override val value: Int): WriteableByte {

    LOGON_CHALLENGE(0x00),
    LOGON_PROOF(0x01),
    REALM_LIST(0x10);

    companion object {
        fun read(input: DataInput): AuthCommand? {
            val int = input.readUnsignedByte()
            return values().find { v -> v.value == int }
        }
    }
}

enum class AuthResult(override val value: Int): WriteableByte {
    SUCCESS(0x00),
    BANNED(0x03),
    UNKNOWN_ACCOUNT(0x04),
    INCORRECT_PASSWORD(0x05)
}
