package net.jammos.realmserver.network

import net.jammos.utils.extensions.toHexString
import net.jammos.utils.types.WriteableByte
import java.io.DataInput

/**
 * Network authentication commands
 */
enum class AuthCommand(override val value: Int): WriteableByte {
    /**
     * Initial challenge.  The client presents an identity, the server presents some secrets.
     */
    LOGON_CHALLENGE(0x00),

    /**
     * Proof in response to the challenge.
     */

    LOGON_PROOF(0x01),

    /**
     * Realm list
     */
    REALM_LIST(0x10);

    override fun toString() = "${super.toString()} (${value.toHexString(3)})"

    companion object {
        /**
         * Read an unsigned byte from the input and convert it to a [AuthCommand]
         */
        fun read(input: DataInput): AuthCommand {
            val int = input.readUnsignedByte()
            return values().find { v -> v.value == int }
                    ?: throw IllegalCommandException(int)
        }
    }

    class IllegalCommandException(cmd: Int): IllegalArgumentException("Illegal command: $cmd (${cmd.toHexString(3)})")

}

enum class AuthResult(override val value: Int): WriteableByte {
    SUCCESS(0x00),
    BANNED(0x03),
    SUSPENDED(0x0C),
    UNKNOWN_ACCOUNT(0x04),
    INCORRECT_PASSWORD(0x05)
}
