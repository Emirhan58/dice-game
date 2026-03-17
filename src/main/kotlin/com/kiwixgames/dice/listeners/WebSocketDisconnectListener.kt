package com.kiwixgames.dice.listeners

import com.kiwixgames.dice.domain.enums.GameStatus
import com.kiwixgames.dice.repositories.GameRepository
import com.kiwixgames.dice.services.GameSessionRegistry
import com.kiwixgames.dice.services.PlayerPresenceService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionDisconnectEvent

/**
 * Marks the player as disconnected when their WebSocket drops.
 * GameTimeoutJob handles event publishing and forfeit after grace period.
 */
@Component
class WebSocketDisconnectListener(
    private val gameSessionRegistry: GameSessionRegistry,
    private val gameRepository: GameRepository,
    private val playerPresenceService: PlayerPresenceService
) {

    private val log = LoggerFactory.getLogger(WebSocketDisconnectListener::class.java)

    @EventListener
    fun handleDisconnect(event: SessionDisconnectEvent) {
        val sessionId = event.sessionId
        val (userId, gameId) = gameSessionRegistry.disconnect(sessionId) ?: return

        try {
            val game = gameRepository.findByIdWithPlayers(gameId)
            if (game == null || game.status != GameStatus.IN_PROGRESS) return

            playerPresenceService.markDisconnected(gameId, userId)
            log.info("Player userId=$userId disconnected from gameId=$gameId — grace period started")
        } catch (e: Exception) {
            log.error("Failed to mark disconnect for gameId=$gameId, userId=$userId: ${e.message}", e)
        }
    }
}
