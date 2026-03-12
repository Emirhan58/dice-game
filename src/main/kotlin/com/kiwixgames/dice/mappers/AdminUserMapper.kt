package com.kiwixgames.dice.mappers

import com.kiwixgames.dice.domain.dtos.admin.AdminUserResponse
import com.kiwixgames.dice.domain.entities.User

object AdminUserMapper {
    fun toDto(user: User, balanceGold: Long): AdminUserResponse = AdminUserResponse(
        id = user.id!!,
        username = user.username,
        email = user.email,
        firstName = user.firstName,
        lastName = user.lastName,
        bio = user.bio,
        profileImage = user.profileImage,
        role = user.role,
        isActive = user.isActive,
        balanceGold = balanceGold,
        createdAt = user.createdAt,
        updatedAt = user.updatedAt
    )
}
