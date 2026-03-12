package com.kiwixgames.dice.services.game.rules

import com.kiwixgames.dice.services.game.ScoringRule
import com.kiwixgames.dice.services.game.ScoringRule.Match

/** Two triplets = 2500 points */
class TwoTripletsRule : ScoringRule {
    override fun matches(counts: IntArray): List<Match> {
        val triplets = (1..6).filter { counts[it] >= 3 }
        if (triplets.size >= 2) {
            // Take the first two triplets found
            val after = counts.copyOf()
            var taken = 0
            for (v in triplets) {
                if (taken >= 2) break
                after[v] -= 3
                taken++
            }
            return listOf(Match(2500, after))
        }
        return emptyList()
    }
}
