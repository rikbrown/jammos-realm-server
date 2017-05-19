package net.jammos.realmserver.utils

import java.io.IOException
import java.security.SecureRandom
import java.util.*

val RANDOM: Random = SecureRandom.getInstanceStrong()

inline fun <T: Any, R> some (value: T?, f: (T) -> R): R?
        = if (value != null) f(value) else null

inline fun <T> checkArgument(value: T, condition: (T) -> Boolean, message: (T) -> String): T {
    checkArgument(condition(value)) { message(value) }
    return value
}

inline fun checkArgument(condition: Boolean, message: () -> String) {
    if (!condition) rejectArgument(message())
}

fun rejectArgument(message: String): Nothing {
    throw IllegalArgumentException(message)
}

inline fun <T> field(field: String, reader: () -> T): T {
    return try {
        reader()
    } catch (e: Exception) {
        throw IOException("Error reading $field", e)
    }
}
