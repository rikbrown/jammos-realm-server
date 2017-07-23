package net.jammos.realmserver.utils.extensions

import java.time.Duration

val Int.minutes: Duration
    get() = Duration.ofMinutes(this.toLong())