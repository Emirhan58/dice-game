package com.kiwixgames.dice.services

import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks player presence via periodic pings.
 * Key: "gameId:userId" → last ping timestamp (epoch ms)
 */
@Service
class PlayerPresenceService {

    private val lastSeen = ConcurrentHashMap<String, Long>()

    fun ping(gameId: Long, userId: Long) {
        lastSeen["$gameId:$userId"] = System.currentTimeMillis()
    }

    fun getLastSeen(gameId: Long, userId: Long): Long? {
        return lastSeen["$gameId:$userId"]
    }

    fun isAbsent(gameId: Long, userId: Long, thresholdMs: Long): Boolean {
        val last = lastSeen["$gameId:$userId"] ?: return false // never pinged = just joined, not absent
        return System.currentTimeMillis() - last > thresholdMs
    }

    fun remove(gameId: Long) {
        lastSeen.keys.removeIf { it.startsWith("$gameId:") }
    }
}
