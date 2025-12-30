package com.kiwixgames.dice.services

import com.kiwixgames.dice.domain.dtos.auth.RegisterRequest
import com.kiwixgames.dice.domain.enums.TokenType
import org.springframework.security.core.userdetails.UserDetails

interface AuthenticationService {
    fun authenticate(email: String, password: String): UserDetails
    fun generateAccessToken(userDetails: UserDetails): String
    fun generateRefreshToken(userDetails: UserDetails): String
    fun generateResetPasswordToken(email: String): String
    fun validateTokenByType(token: String, expectedType: TokenType): UserDetails
    fun validateTokenForFilter(token: String): UserDetails
    fun extractExpirationMillis(token: String): Long

    fun register(request: RegisterRequest): UserDetails
    fun resetPassword(token: String, newPassword: String)
}