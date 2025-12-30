package com.kiwixgames.dice.domain.data

import com.kiwixgames.dice.domain.enums.BadgeTier
import com.kiwixgames.dice.domain.enums.TableMode

data class TableRules(
    val mode: TableMode,
    val badgeTier: BadgeTier?,
    val stakeGold: Int,
    val targetScore: Int
)