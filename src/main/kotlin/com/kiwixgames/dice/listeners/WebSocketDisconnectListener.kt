package com.kiwixgames.dice.listeners

import com.kiwixgames.dice.domain.enums.GameStatus
import com.kiwixgames.dice.repositories.GameRepository
import com.kiwixgames.dice.repositories.UserRepository
import com.kiwixgames.dice.services.GamePlayService
import com.kiwixgames.dice.services.GameSessionRegistry
import com.kiwixgames.dice.services.PlayerPresenceService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionDisconnectEvent

/**
 * When a player's WebSocket disconnects, immediately forfeit their active game.
 */
@Component
class WebSocketDisconnectListener(
    private val gameSessionRegistry: GameSessionRegistry,
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository,
    @Lazy private val gamePlayService: GamePlayService,
    private val playerPresenceService: PlayerPresenceService
) {

    private val log = LoggerFactory.getLogger(WebSocketDisconnectListener::class.java)

    @EventListener
    fun handleDisconnect(event: SessionDisconnectEvent) {
        val sessionId = event.sessionId
        log.info("SessionDisconnectEvent received: sessionId=$sessionId")
        val pair = gameSessionRegistry.disconnect(sessionId)
        if (pair == null) {
            log.info("No game session found for sessionId=$sessionId — ignoring disconnect")
            return
        }
        val (userId, gameId) = pair

        log.info("Player userId=$userId disconnected from gameId=$gameId. Forfeiting immediately.")

        try {
            val game = gameRepository.findByIdWithPlayers(gameId)
            if (game == null || game.status != GameStatus.IN_PROGRESS) return

            val user = userRepository.findById(userId).orElse(null) ?: return

            gamePlayService.forfeit(gameId, user, "DISCONNECT")
            playerPresenceService.remove(gameId)
            log.info("Player userId=$userId forfeited gameId=$gameId due to disconnect.")
        } catch (e: Exception) {
            log.error("Failed to forfeit gameId=$gameId for userId=$userId on disconnect: ${e.message}", e)
        }
    }

    fun cancelPendingForfeit(userId: Long) {
        // kept for API compatibility with WebSocketAuthInterceptor
    }
}
