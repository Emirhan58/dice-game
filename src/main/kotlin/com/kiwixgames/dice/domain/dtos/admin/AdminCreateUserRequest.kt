package com.kiwixgames.dice.domain.dtos.admin

import com.kiwixgames.dice.domain.enums.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AdminCreateUserRequest(
    @field:NotBlank
    @field:Size(min = 3, max = 20)
    val username: String,

    @field:NotBlank
    @field:Email
    val email: String,

    @field:NotBlank
    @field:Size(min = 8)
    val password: String,

    val firstName: String? = null,
    val lastName: String? = null,
    val role: Role = Role.USER,
    val balanceGold: Long = 0
)
