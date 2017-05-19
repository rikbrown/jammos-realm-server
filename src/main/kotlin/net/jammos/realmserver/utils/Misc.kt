package net.jammos.realmserver.utils

import java.security.SecureRandom
import java.util.*

inline fun <T: Any, R> some (value: T?, f: (T) -> R): R?
        = if (value != null) f(value) else null

val RANDOM: Random = SecureRandom.getInstanceStrong()

fun checkArgument(value: Boolean, message: () -> String) {
    if (!value) rejectArgument(message())
}

fun rejectArgument(message: String): Nothing {
    throw IllegalArgumentException(message)
}