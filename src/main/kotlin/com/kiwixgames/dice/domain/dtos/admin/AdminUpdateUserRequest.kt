package com.kiwixgames.dice.domain.dtos.admin

import com.kiwixgames.dice.domain.enums.Role
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

data class AdminUpdateUserRequest(
    @field:Size(min = 3, max = 20)
    val username: String? = null,

    @field:Email
    val email: String? = null,

    @field:Size(min = 8)
    val password: String? = null,

    val firstName: String? = null,
    val lastName: String? = null,
    val role: Role? = null,
    val isActive: Boolean? = null
)
