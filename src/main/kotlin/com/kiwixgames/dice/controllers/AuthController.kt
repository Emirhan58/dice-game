package com.kiwixgames.dice.controllers

import com.kiwixgames.dice.domain.dtos.ApiResponse
import com.kiwixgames.dice.domain.dtos.auth.AccessTokenResponse
import com.kiwixgames.dice.domain.dtos.auth.LoginRequest
import com.kiwixgames.dice.domain.dtos.auth.LoginResponse
import com.kiwixgames.dice.domain.dtos.auth.RegisterRequest
import com.kiwixgames.dice.domain.enums.TokenType
import com.kiwixgames.dice.mappers.AuthMapper
import com.kiwixgames.dice.services.AuthenticationService
import com.kiwixgames.dice.services.TokenBlacklistService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import kotlin.text.isNullOrBlank
import kotlin.text.removePrefix
import kotlin.text.startsWith
import kotlin.text.trim

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "Authorization Operations")
class AuthController(
    private val authenticationService: AuthenticationService,
    private val tokenBlacklistService: TokenBlacklistService
) {

    @PostMapping("/login")
    fun login(@RequestBody loginRequest: LoginRequest): ResponseEntity<LoginResponse> {
        val userDetails: UserDetails = authenticationService.authenticate(
            loginRequest.email,
            loginRequest.password
        )
        val refreshToken = authenticationService.generateRefreshToken(userDetails)

        val cookie = ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(7 * 24 * 60 * 60) // 7 days
            .sameSite("Strict")
            .build()

        val accessToken = authenticationService.generateAccessToken(userDetails)
        val dto: LoginResponse = AuthMapper.toLoginDto(accessToken, refreshToken)
        return ResponseEntity
            .ok()
            .header("Set-Cookie", cookie.toString())
            .body(dto)
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<ApiResponse> {
        authenticationService.register(request)
        val response = ApiResponse(status = 201, message = "User registered successfully")
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response)
    }

    @PostMapping("/refresh")
    fun refresh(@CookieValue("refreshToken") refreshToken: String?): ResponseEntity<AccessTokenResponse> {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val userDetails = authenticationService.validateTokenByType(refreshToken, TokenType.REFRESH)
        val newAccessToken = authenticationService.generateAccessToken(userDetails)

        val response = AccessTokenResponse(
            accessToken = newAccessToken,
            expiresIn = 900
        )
        return ResponseEntity.ok(response)
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Logs out the current user by blacklisting the access token and clearing the refresh token cookie."
    )
    fun logout(
        request: HttpServletRequest,
        @Parameter(hidden = false) @CookieValue("refreshToken") refreshToken: String? = null
    ): ResponseEntity<Void> {
        val authorizationHeader = request.getHeader("Authorization")
        if (authorizationHeader.isNullOrBlank() || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }

        val accessToken = authorizationHeader.removePrefix("Bearer ").trim()
        val accessTokenExpiry = authenticationService.extractExpirationMillis(accessToken)
        tokenBlacklistService.blacklistToken(accessToken, accessTokenExpiry)

        if(refreshToken != null){
            val refreshTokenExpiry = authenticationService.extractExpirationMillis(refreshToken)
            tokenBlacklistService.blacklistToken(refreshToken,refreshTokenExpiry)
        }

        val expiredCookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .secure(true)
            .path("/")
            .maxAge(0)
            .build()

        return ResponseEntity
            .status(HttpStatus.OK)
            .header("Set-Cookie", expiredCookie.toString())
            .build()
    }
}

