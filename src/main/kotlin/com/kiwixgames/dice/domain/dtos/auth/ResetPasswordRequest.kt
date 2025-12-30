package com.kiwixgames.dice.domain.dtos.auth

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ResetPasswordRequest(

    @field:NotBlank(message = "Reset token is required")
    val token: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters long")
    val newPassword: String
)