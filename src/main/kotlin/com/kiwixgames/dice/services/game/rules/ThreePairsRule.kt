package com.kiwixgames.dice.services.game.rules

import com.kiwixgames.dice.services.game.ScoringRule
import com.kiwixgames.dice.services.game.ScoringRule.Match

/**
 * Three pairs = 1500 points.
 * Includes four-of-a-kind + one pair (the quad counts as two pairs).
 */
class ThreePairsRule : ScoringRule {
    override fun matches(counts: IntArray): List<Match> {
        val pairs = mutableListOf<Int>()
        val temp = counts.copyOf()
        for (v in 1..6) {
            while (temp[v] >= 2 && pairs.size < 3) {
                pairs.add(v)
                temp[v] -= 2
            }
        }
        if (pairs.size == 3) {
            val after = counts.copyOf()
            pairs.forEach { v -> after[v] -= 2 }
            return listOf(Match(1500, after))
        }
        return emptyList()
    }
}
