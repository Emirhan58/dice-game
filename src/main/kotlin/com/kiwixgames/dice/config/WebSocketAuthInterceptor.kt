package com.kiwixgames.dice.config

import com.kiwixgames.dice.domain.enums.TokenType
import com.kiwixgames.dice.repositories.GameRepository
import com.kiwixgames.dice.security.BlogUserDetails
import com.kiwixgames.dice.services.AuthenticationService
import com.kiwixgames.dice.services.TokenBlacklistService
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
    private val gameRepository: GameRepository
) : ChannelInterceptor {

    private val gameTopicPattern = Regex("^/topic/games/(\\d+)$")

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
            ?: return message

        when (accessor.command) {
            StompCommand.CONNECT -> handleConnect(accessor)
            StompCommand.SUBSCRIBE -> handleSubscribe(accessor)
            else -> {}
        }

        return message
    }

    private fun handleConnect(accessor: StompHeaderAccessor) {
        val token = accessor.getFirstNativeHeader("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()

        if (token.isNullOrBlank()) {
            throw IllegalArgumentException("Missing authentication token")
        }

        if (tokenBlacklistService.isTokenBlacklisted(token)) {
            throw IllegalArgumentException("Token is blacklisted")
        }

        val userDetails = authenticationService.validateTokenByType(token, TokenType.ACCESS)
        val authentication = UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.authorities
        )
        accessor.user = authentication
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

        val game = gameRepository.findById(gameId).orElseThrow {
            IllegalArgumentException("Game not found: $gameId")
        }
        val table = game.table
        if (table.seat0?.id != userId && table.seat1?.id != userId) {
            throw IllegalArgumentException("You are not a player in this game")
        }
    }
}
