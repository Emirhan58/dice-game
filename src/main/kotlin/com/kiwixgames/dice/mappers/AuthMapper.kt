package com.kiwixgames.dice.mappers

import com.kiwixgames.dice.domain.dtos.auth.LoginResponse


object AuthMapper {
    fun toLoginDto(accessToken: String, refreshToken:String) = LoginResponse(
        accessToken = accessToken,
        refreshToken = refreshToken
    )
}