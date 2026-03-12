package com.kiwixgames.dice.services.game.rules

import com.kiwixgames.dice.services.game.ScoringRule
import com.kiwixgames.dice.services.game.ScoringRule.Match

/**
 * N-of-a-kind with doubling.
 *
 * Three of a kind: 1→1000, N→N×100
 * Four of a kind:  ×2
 * Five of a kind:  ×4
 * Six of a kind:   ×8
 */
class NOfAKindRule : ScoringRule {
    override fun matches(counts: IntArray): List<Match> {
        val result = mutableListOf<Match>()
        for (v in 1..6) {
            if (counts[v] < 3) continue
            val base = if (v == 1) 1000 else v * 100
            for (n in 3..minOf(6, counts[v])) {
                val multiplier = 1 shl (n - 3) // 3→×1, 4→×2, 5→×4, 6→×8
                val after = counts.copyOf().also { it[v] -= n }
                result.add(Match(base * multiplier, after))
            }
        }
        return result
    }
}
