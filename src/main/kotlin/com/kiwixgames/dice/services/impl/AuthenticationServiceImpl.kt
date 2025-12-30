package com.kiwixgames.dice.services.impl


import com.kiwixgames.dice.domain.dtos.auth.RegisterRequest
import com.kiwixgames.dice.domain.entities.User
import com.kiwixgames.dice.domain.enums.Role
import com.kiwixgames.dice.domain.enums.TokenType
import com.kiwixgames.dice.repositories.UserRepository
import com.kiwixgames.dice.security.BlogUserDetails
import com.kiwixgames.dice.services.AuthenticationService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.persistence.EntityNotFoundException
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class AuthenticationServiceImpl(
    private val authenticationManager: AuthenticationManager,
    private val userDetailsService: UserDetailsService,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : AuthenticationService {

    @Value("\${jwt.secret}")
    private lateinit var secretKey: String

    private val accessTokenExpiryMs: Long = 15 * 60 * 1000L  // 15 minutes
    private val refreshTokenExpiryMs: Long = 7 * 24 * 60 * 60 * 1000L // 7 days
    private val resetPasswordTokenExpiryMs: Long = 10 * 60 * 1000L // 10 minutes

    override fun authenticate(email: String, password: String): UserDetails {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(email, password)
        )
        return userDetailsService.loadUserByUsername(email)
    }

    override fun generateAccessToken(userDetails: UserDetails): String {
        return generateToken(userDetails.username, accessTokenExpiryMs, TokenType.ACCESS)
    }

    override fun generateRefreshToken(userDetails: UserDetails): String {
        return generateToken(userDetails.username, refreshTokenExpiryMs, TokenType.REFRESH)
    }

    override fun generateResetPasswordToken(email: String): String {
        return generateToken(email, resetPasswordTokenExpiryMs, TokenType.RESET)
    }

    private fun generateToken(subject: String, expiry: Long, tokenType: TokenType): String {
        val now = System.currentTimeMillis()
        return Jwts.builder()
            .subject(subject)
            .issuedAt(Date(now))
            .expiration(Date(now + expiry))
            .claim("token_type", tokenType.toString())
            .signWith(getSigningKey())
            .compact()
    }

    override fun validateTokenByType(token: String, expectedType: TokenType): UserDetails {
        val claims = parseClaims(token)
        val tokenType = claims["token_type"] as? String
            ?: throw IllegalArgumentException("Token type claim is missing")

        val expectedTypeStr: String = expectedType.toString()

        if (tokenType != expectedTypeStr) {
            throw IllegalArgumentException("Invalid token type: expected $expectedTypeStr but was $tokenType")
        }

        val username = claims.subject
        return userDetailsService.loadUserByUsername(username)
    }

    override fun validateTokenForFilter(token: String): UserDetails {
        val username = extractUsername(token)
        return userDetailsService.loadUserByUsername(username)
    }

    override fun register(request: RegisterRequest): UserDetails {
        if (userRepository.findByEmail(request.email) != null) {
            throw IllegalArgumentException("Email already in use")
        }
        if (userRepository.findByUsername(request.username) != null) {
            throw IllegalArgumentException("Username already taken")
        }

        val user = User(
            username = request.username,
            email = request.email,
            password = passwordEncoder.encode(request.password)!!,
            firstName = request.firstName,
            lastName = request.lastName,
            role = Role.USER,
            isActive = true
        )

        return BlogUserDetails(userRepository.save(user))
    }

    override fun resetPassword(token: String, newPassword: String) {
        val userDetails: UserDetails = validateTokenByType(token, TokenType.RESET)
        val email: String = userDetails.username
        val user = userRepository.findByEmail(email)
            ?: throw EntityNotFoundException("User not found with email: $email")

        user.password = passwordEncoder.encode(newPassword)!!
        userRepository.save(user)
    }

    override fun extractExpirationMillis(token: String): Long {
        val claims = parseClaims(token)
        return claims.expiration.time - System.currentTimeMillis()
    }

    private fun extractUsername(token: String): String {
        return parseClaims(token).subject
    }

    private fun parseClaims(token: String) = try {
        Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .payload
    } catch (ex: Exception) {
        throw IllegalArgumentException("Failed to parse JWT: ${ex.message}")
    }

    private fun getSigningKey(): SecretKey {
        return Keys.hmacShaKeyFor(secretKey.toByteArray())
    }
}