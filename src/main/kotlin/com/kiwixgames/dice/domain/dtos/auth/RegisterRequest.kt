package com.kiwixgames.dice.domain.dtos.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(

    @field:NotBlank(message = "Username is required")
    @field:Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    val username: String,

    @field:Email(message = "Please provide a valid email address")
    @field:NotBlank(message = "Email is required")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters long")
    val password: String,

    val firstName: String? = null,
    val lastName: String? = null
)