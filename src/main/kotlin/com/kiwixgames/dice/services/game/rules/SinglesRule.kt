package com.kiwixgames.dice.services.game.rules

import com.kiwixgames.dice.services.game.ScoringRule
import com.kiwixgames.dice.services.game.ScoringRule.Match

/** Single 1 = 100, Single 5 = 50 */
class SinglesRule : ScoringRule {
    override fun matches(counts: IntArray): List<Match> {
        val result = mutableListOf<Match>()
        if (counts[1] > 0) {
            val after = counts.copyOf().also { it[1]-- }
            result.add(Match(100, after))
        }
        if (counts[5] > 0) {
            val after = counts.copyOf().also { it[5]-- }
            result.add(Match(50, after))
        }
        return result
    }
}
