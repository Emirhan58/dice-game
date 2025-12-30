package com.kiwixgames.dice.domain.dtos.auth

data class AccessTokenResponse(
    val accessToken: String,
    val expiresIn: Long
)