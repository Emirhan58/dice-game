package com.kiwixgames.dice.domain.dtos.auth

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String
)