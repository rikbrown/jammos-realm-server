package net.jammos.realmserver.utils

import java.security.SecureRandom
import java.util.*

inline fun <T: Any, R> some (value: T?, f: (T) -> R): R?
        = if (value != null) f(value) else null

val RANDOM: Random = SecureRandom.getInstanceStrong()
