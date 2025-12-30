package com.kiwixgames.dice.services.rules

import com.kiwixgames.dice.domain.data.TableRules
import com.kiwixgames.dice.domain.enums.BadgeTier
import com.kiwixgames.dice.domain.enums.TableMode

class RulesEngine {
    fun resolve(mode: TableMode, badgeTier: BadgeTier?, stakeGold: Int): TableRules {
        return when (mode) {
            TableMode.UNBADGED -> {
                val target = when (stakeGold) {
                    5 -> 1500
                    30 -> 4000
                    else -> throw IllegalArgumentException("UNBADGED stake only supports 5 or 30 (given: $stakeGold)")
                }
                TableRules(mode, null, stakeGold, target)
            }
            TableMode.BADGED -> {
                val tier = badgeTier ?: throw IllegalArgumentException("badgeTier is required for BADGED tables")
                val target = when (tier) {
                    BadgeTier.TIN -> 4000
                    BadgeTier.SILVER -> 5000
                    BadgeTier.GOLD -> 8000
                }
                TableRules(mode, tier, stakeGold, target)
            }
        }
    }
}