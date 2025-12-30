package com.kiwixgames.dice.domain.dtos.auth

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresInRefreshToken: Long,
    val expiresInAccessToken: Long
)