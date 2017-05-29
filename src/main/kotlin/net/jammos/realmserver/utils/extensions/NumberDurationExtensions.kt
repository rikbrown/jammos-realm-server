package net.jammos.realmserver.utils.extensions

import java.time.Duration

val Number.seconds
        get() = Duration.ofSeconds(this.toLong())!!
