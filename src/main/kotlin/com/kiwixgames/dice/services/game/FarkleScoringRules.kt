package com.kiwixgames.dice.services.game

import com.kiwixgames.dice.services.game.rules.*

/**
 * Registry of active scoring rules.
 *
 * To add a new rule:
 *   1. Create a class implementing [ScoringRule]
 *   2. Add it to the list below
 *
 * Order does not matter — the engine tries all rules and picks
 * the combination that maximizes the total score.
 */
object FarkleScoringRules {
    val defaultRules: List<ScoringRule> = listOf(
        SinglesRule(),
        NOfAKindRule(),
        StraightRule(),
        ThreePairsRule(),
        TwoTripletsRule()
    )
}
