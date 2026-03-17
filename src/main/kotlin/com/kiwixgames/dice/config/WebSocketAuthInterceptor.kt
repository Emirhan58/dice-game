package com.kiwixgames.dice.config

import com.kiwixgames.dice.domain.enums.TokenType
import com.kiwixgames.dice.repositories.GameRepository
import com.kiwixgames.dice.security.BlogUserDetails
import com.kiwixgames.dice.services.AuthenticationService
import com.kiwixgames.dice.services.GameSessionRegistry
import com.kiwixgames.dice.services.PlayerPresenceService
import com.kiwixgames.dice.services.TokenBlacklistService
import org.slf4j.LoggerFactory
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component

@Component
class WebSocketAuthInterceptor(
    private val authenticationService: AuthenticationService,
    private val tokenBlacklistService: TokenBlacklistService,
    private val gameRepository: GameRepository,
    private val gameSessionRegistry: GameSessionRegistry,
    private val playerPresenceService: PlayerPresenceService
) : ChannelInterceptor {

    private val log = LoggerFactory.getLogger(WebSocketAuthInterceptor::class.java)

    private val gameTopicPattern = Regex("^/topic/games/(\\d+)$")

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
            ?: return message

        val command = accessor.command ?: return message

        when (command) {
            StompCommand.CONNECT -> handleConnect(accessor)
            StompCommand.SUBSCRIBE -> handleSubscribe(accessor)
            else -> {}
        }

        return message
    }

    private fun handleConnect(accessor: StompHeaderAccessor) {
        try {
            val token = accessor.getFirstNativeHeader("Authorization")
                ?.removePrefix("Bearer ")
                ?.trim()

            if (token.isNullOrBlank()) {
                log.warn("CONNECT failed: Missing authentication token, sessionId=${accessor.sessionId}")
                throw IllegalArgumentException("Missing authentication token")
            }

            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                log.warn("CONNECT failed: Token blacklisted, sessionId=${accessor.sessionId}")
                throw IllegalArgumentException("Token is blacklisted")
            }

            val userDetails = authenticationService.validateTokenByType(token, TokenType.ACCESS)
            val authentication = UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.authorities
            )
            accessor.user = authentication
            log.info("CONNECT success: userId=${(userDetails as? BlogUserDetails)?.getId()}, sessionId=${accessor.sessionId}")
        } catch (e: Exception) {
            log.error("CONNECT error: ${e.message}, sessionId=${accessor.sessionId}")
            throw e
        }
    }

    private fun handleSubscribe(accessor: StompHeaderAccessor) {
        val destination = accessor.destination ?: throw IllegalArgumentException("No destination")
        val match = gameTopicPattern.matchEntire(destination) ?: return

        val gameId = match.groupValues[1].toLongOrNull()
            ?: throw IllegalArgumentException("Invalid game id")

        val principal = accessor.user as? UsernamePasswordAuthenticationToken
            ?: throw IllegalArgumentException("Not authenticated")
        val userDetails = principal.principal as? BlogUserDetails
            ?: throw IllegalArgumentException("Not authenticated")
        val userId = userDetails.getId()

        val game = gameRepository.findByIdWithPlayers(gameId)
            ?: throw IllegalArgumentException("Game not found: $gameId")
        val table = game.table
        if (table.seat0?.id != userId && table.seat1?.id != userId) {
            throw IllegalArgumentException("You are not a player in this game")
        }

        val sessionId = accessor.sessionId
        if (sessionId != null && userId != null) {
            gameSessionRegistry.connect(sessionId, userId, gameId)
            playerPresenceService.clearDisconnect(gameId, userId)
            log.info("Session registered: sessionId=$sessionId, userId=$userId, gameId=$gameId")
        } else {
            log.warn("Session NOT registered: sessionId=$sessionId, userId=$userId, gameId=$gameId")
        }
    }

    // DISCONNECT is handled by WebSocketDisconnectListener via SessionDisconnectEvent.
}
