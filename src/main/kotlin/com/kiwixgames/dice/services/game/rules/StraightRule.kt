package com.kiwixgames.dice.services.game.rules

import com.kiwixgames.dice.services.game.ScoringRule
import com.kiwixgames.dice.services.game.ScoringRule.Match

/** Full straight 1-2-3-4-5-6 = 3000 points */
class StraightRule : ScoringRule {
    override fun matches(counts: IntArray): List<Match> {
        if ((1..6).all { counts[it] > 0 }) {
            val after = counts.copyOf().also { c -> (1..6).forEach { c[it]-- } }
            return listOf(Match(3000, after))
        }
        return emptyList()
    }
}
