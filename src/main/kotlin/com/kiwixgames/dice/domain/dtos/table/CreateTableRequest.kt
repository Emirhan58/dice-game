package com.kiwixgames.dice.domain.dtos.table

import com.kiwixgames.dice.domain.enums.BadgeTier
import com.kiwixgames.dice.domain.enums.TableMode
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull


data class CreateTableRequest(
    @field:NotNull
    var mode: TableMode,

    val badgeTier: BadgeTier? = null,

    @field:Min(1)
    val stakeGold: Int
)