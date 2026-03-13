package com.kiwixgames.dice.services

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks which users are connected to which games via WebSocket.
 * userId → gameId mapping.
 */
@Component
class GameSessionRegistry {

    private val userToGame = ConcurrentHashMap<Long, Long>()
    private val sessionToUser = ConcurrentHashMap<String, Long>()

    fun connect(sessionId: String, userId: Long, gameId: Long) {
        sessionToUser[sessionId] = userId
        userToGame[userId] = gameId
    }

    fun disconnect(sessionId: String): Pair<Long, Long>? {
        val userId = sessionToUser.remove(sessionId) ?: return null
        val gameId = userToGame.remove(userId) ?: return null
        return userId to gameId
    }

    fun isConnected(userId: Long): Boolean {
        return userToGame.containsKey(userId)
    }
}
