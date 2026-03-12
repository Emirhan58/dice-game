package com.kiwixgames.dice.domain.dtos.admin

import com.kiwixgames.dice.domain.enums.Role
import java.time.LocalDateTime

data class AdminUserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val bio: String?,
    val profileImage: String?,
    val role: Role,
    val isActive: Boolean,
    val balanceGold: Long,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
