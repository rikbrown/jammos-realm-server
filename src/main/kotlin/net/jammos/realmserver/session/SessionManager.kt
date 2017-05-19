package net.jammos.realmserver.session

import mu.KLogging
import java.time.Clock
import java.time.Duration
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

/**
 * An in-memory implementation of a session manager
 */
class InMemorySessionManager(private val clock: Clock = Clock.systemUTC()): SessionManager {
    companion object: KLogging() {
        /**
         * Duration until a session is considered dead, even if resumed
         */
        private val SESSION_EXPIRY_TIMEOUT = Duration.ofMinutes(5)
    }

    private val sessionMap: ConcurrentMap<SessionId, Session> = ConcurrentHashMap()

    override fun registerSession(sessionId: SessionId): Session {
        logger.debug { "Registering new/existing session: $sessionId" }
        val session = sessionMap[sessionId]
                // if present, update ping
                ?.copyPinged(clock)
                // or create new
                ?: Session(sessionId, now(clock))

        sessionMap[sessionId] = session

        return session
    }

    override fun resumeSession(sessionId: SessionId): Session? {
        logger.debug { "Marking $sessionId as resumed" }
        val session = getSession(sessionId)
                // if present, update ping
                ?.copyPinged(clock)
        sessionMap[sessionId] = session
        return session
    }

    override fun closeSession(sessionId: SessionId) {
        logger.debug { "Closing session: $sessionId" }
        sessionMap.remove(sessionId)
    }

    private fun getSession(sessionId: SessionId): Session? = sessionMap[sessionId]
            // retrieve session as long as it has not expired
            ?.takeIf { it.lastPingedAt > now(clock) - SESSION_EXPIRY_TIMEOUT }

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