package com.kiwixgames.dice.domain.dtos.table

data class TableResponse(
    val id: Long,
    val status: String,
    val mode: String,
    val badgeTier: String?,
    val stakeGold: Int,
    val targetScore: Int,
    val seat0UserId: Long?,
    val seat1UserId: Long?
)