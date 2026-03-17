package com.kiwixgames.dice.services

import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks player presence via periodic pings and WebSocket disconnect timestamps.
 * Pure data service — no event publishing or business logic.
 */
@Service
class PlayerPresenceService {

    private val lastSeen = ConcurrentHashMap<String, Long>()
    private val disconnectedAt = ConcurrentHashMap<String, Long>()

    fun ping(gameId: Long, userId: Long) {
        lastSeen["$gameId:$userId"] = System.currentTimeMillis()
    }

    fun getLastSeen(gameId: Long, userId: Long): Long? {
        return lastSeen["$gameId:$userId"]
    }

    fun isAbsent(gameId: Long, userId: Long, thresholdMs: Long): Boolean {
        val last = lastSeen["$gameId:$userId"] ?: return false
        return System.currentTimeMillis() - last > thresholdMs
    }

    fun markDisconnected(gameId: Long, userId: Long) {
        disconnectedAt["$gameId:$userId"] = System.currentTimeMillis()
    }

    /**
     * Clears disconnect state (player reconnected).
     * Returns true if there was an active disconnect to clear.
     */
    fun clearDisconnect(gameId: Long, userId: Long): Boolean {
        return disconnectedAt.remove("$gameId:$userId") != null
    }

    fun isDisconnected(gameId: Long, userId: Long): Boolean {
        return disconnectedAt.containsKey("$gameId:$userId")
    }

    fun isDisconnectedBeyondGrace(gameId: Long, userId: Long, graceMs: Long): Boolean {
        val dcTime = disconnectedAt["$gameId:$userId"] ?: return false
        return System.currentTimeMillis() - dcTime > graceMs
    }

    fun remove(gameId: Long) {
        val prefix = "$gameId:"
        lastSeen.keys.removeIf { it.startsWith(prefix) }
        disconnectedAt.keys.removeIf { it.startsWith(prefix) }
    }
}
