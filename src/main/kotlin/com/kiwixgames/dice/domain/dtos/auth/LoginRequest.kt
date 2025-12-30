package com.kiwixgames.dice.domain.dtos.auth

import io.swagger.v3.oas.annotations.media.Schema

data class LoginRequest(
    @field:Schema(description = "email", example = " ")
    val email: String,
    @field:Schema(description = "password", example = " ")
    val password: String
)