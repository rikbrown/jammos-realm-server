package net.jammos.realmserver.data

import net.jammos.realmserver.utils.extensions.readChars
import net.jammos.realmserver.utils.extensions.readIpAddress
import net.jammos.realmserver.utils.extensions.readUnsignedInt
import java.io.DataInput
import java.net.InetAddress

data class AuthLogonChallenge(
        val gameName: String,
        val version1: Int,
        val version2: Int,
        val version3: Int,
        val build: Int,
        val platform: String,
        val os: String,
        val country: String,
        val timezoneBias: Int,
        val ip: InetAddress,
        val srpIdentity: String

) {

    companion object {

        fun read(input: DataInput): AuthLogonChallenge {
            return AuthLogonChallenge(
                    gameName = input.readChars(4, reverse = false),
                    version1 = input.readUnsignedByte(),
                    version2 = input.readUnsignedByte(),
                    version3 = input.readUnsignedByte(),
                    build = input.readUnsignedShort(),
                    platform = input.readChars(4),
                    os = input.readChars(4),
                    country = input.readChars(4),
                    timezoneBias = input.readUnsignedInt(),
                    ip = input.readIpAddress(),
                    // FIXME: validate the length isn't ridiculous
                    srpIdentity = input.readChars(input.readUnsignedByte(), reverse = false))
        }

    }

}
