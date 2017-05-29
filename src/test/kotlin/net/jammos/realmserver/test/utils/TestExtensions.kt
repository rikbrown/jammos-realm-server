package net.jammos.realmserver.test.utils

import java.io.DataInput

/**
 * drop a byte to account for the command which is read earlier
 */
fun DataInput.dropOneByte(): DataInput {
    readByte()
    return this
}


