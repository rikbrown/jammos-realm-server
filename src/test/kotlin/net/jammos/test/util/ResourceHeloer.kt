package net.jammos.test.util

import org.jetbrains.spek.api.dsl.SpecBody
import org.jetbrains.spek.api.lifecycle.CachingMode
import org.jetbrains.spek.api.lifecycle.LifecycleAware
import redis.embedded.Redis
import redis.embedded.RedisServer
import java.io.Closeable

fun <T: Closeable> SpecBody.resource(mode: CachingMode = CachingMode.TEST, factory: () -> T): LifecycleAware<T> {
    val memoized = memoized(mode, factory)
    val resource by memoized
    when (mode) {
        CachingMode.TEST -> afterEachTest { resource.close() }
        CachingMode.GROUP -> afterGroup { resource.close() }
        else -> throw UnsupportedOperationException()
    }
    return memoized
}

class CloseableRedis(val redis: RedisServer): Redis by redis, Closeable {
    override fun close() {
        System.err.println("Shutting down redis")
        redis.stop()
    }
}