package com.kiwixgames.dice.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.kiwixgames.dice.domain.dtos.ApiErrorResponse
import com.kiwixgames.dice.domain.enums.TokenType
import com.kiwixgames.dice.services.AuthenticationService
import com.kiwixgames.dice.services.TokenBlacklistService
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.filter.OncePerRequestFilter
import java.io.IOException

class JwtAuthenticationFilter(
    private val authenticationService: AuthenticationService,
    private val tokenBlacklistService: TokenBlacklistService
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)
    private val errorObjectMapper = ObjectMapper()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return path.startsWith("/api/v1/auth/refresh")
                || path.startsWith("/api/v1/auth/reset-password")
    }

    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = extractToken(request) ?: run {
                filterChain.doFilter(request, response)
                return
            }

            if (tokenBlacklistService.isTokenBlacklisted(token)) {
                log.warn("Token is blacklisted")
                sendJsonErrorResponse(
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "Token is blacklisted"
                )
                return
            }

            val userDetails: UserDetails = authenticationService.validateTokenByType(token, TokenType.ACCESS)

            if (!userDetails.isEnabled) {
                sendJsonErrorResponse(response, HttpStatus.UNAUTHORIZED, "Account is deactivated")
                return
            }

            val authentication = UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.authorities
            )
            SecurityContextHolder.getContext().authentication = authentication

            if (userDetails is BlogUserDetails) {
                request.setAttribute("userId", userDetails.getId())
            }

        } catch (ex: IllegalArgumentException) {
            log.warn("Invalid token: ${ex.message}")
            sendJsonErrorResponse(
                response,
                HttpStatus.BAD_REQUEST,
                ex.message ?: "Invalid token"
            )
            return
        } catch (ex: Exception) {
            log.warn("Authentication error: $ex")
            sendJsonErrorResponse(
                response,
                HttpStatus.UNAUTHORIZED,
                "Authentication failed"
            )
            return
        }

        filterChain.doFilter(request, response)
    }


    private fun extractToken(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        return if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            bearerToken.substring(7)
        } else null
    }

    private fun sendJsonErrorResponse(
        response: HttpServletResponse,
        status: HttpStatus,
        message: String
    ) {
        response.status = status.value()
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"

        val errorResponse = ApiErrorResponse(
            status = status.value(),
            message = message,
            errors = null
        )

        val writer = response.writer
        writer.write(errorObjectMapper.writeValueAsString(errorResponse))
        writer.flush()
    }

}
