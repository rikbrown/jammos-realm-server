package net.jammos.realmserver.session

import mu.KLogging
import java.time.Clock
import java.time.Instant
import java.time.Instant.now
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

interface SessionManager {
    fun registerSession(sessionId: SessionId): Session
    fun resumeSession(sessionId: SessionId): Session?
    fun closeSession(sessionId: SessionId)
}

class InMemorySessionManager(private val clock: Clock = Clock.systemUTC()): SessionManager {
    companion object: KLogging()

    private val sessionMap: ConcurrentMap<SessionId, Session> = ConcurrentHashMap()

    override fun registerSession(sessionId: SessionId): Session {
        logger.debug { "Registering new/existing sessionId($sessionId)" }
        val session = sessionMap[sessionId]
                // if present, update ping
                ?.copyPinged(clock)
                // or create new
                ?: Session(sessionId, now(clock))

        sessionMap[sessionId] = session

        return session
    }

    override fun resumeSession(sessionId: SessionId): Session? {
        sessionMap[sessionId] = sessionMap[sessionId]
                // if present, update ping
                ?.copyPinged(clock)
    }

    override fun closeSession(sessionId: SessionId) {
        logger.debug { "Closing sessionId($sessionId)" }
        sessionMap.remove(sessionId)
    }

}


data class Session(
        val sessionId: SessionId,
        val startedAt: Instant,
        val lastPingedAt: Instant
) {
    constructor(sessionId: SessionId, now: Instant): this(sessionId, now, now)

    fun copyPinged(clock: Clock): Session = this.copy(lastPingedAt = now(clock))

}

data class SessionId(val sessionId: String) {
    companion object {
        fun generate(): SessionId {
            return SessionId("j-" + UUID.randomUUID().toString())
        }
    }
}