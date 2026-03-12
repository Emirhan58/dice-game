package com.kiwixgames.dice.domain.dtos.admin

import jakarta.validation.constraints.NotNull

data class AdminAdjustBalanceRequest(
    @field:NotNull
    val amount: Long,

    val note: String? = null
)
