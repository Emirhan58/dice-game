package com.kiwixgames.dice.config

import com.kiwixgames.dice.domain.enums.TokenType
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
    private val tokenBlacklistService: TokenBlacklistService
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java)
            ?: return message

        if (accessor.command == StompCommand.CONNECT) {
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

        return message
    }
}
