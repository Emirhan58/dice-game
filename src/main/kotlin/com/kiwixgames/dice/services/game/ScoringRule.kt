package com.kiwixgames.dice.services.game

/**
 * A single Farkle scoring rule.
 *
 * Each rule inspects the current dice counts and returns all possible
 * "matches" it can find. A match is a (points, consumed) pair where
 * [consumed] is the count array after removing the matched dice.
 *
 * To add a new rule: implement this interface and register it in
 * [FarkleScoringRules.defaultRules].
 */
interface ScoringRule {
    fun matches(counts: IntArray): List<Match>

    data class Match(
        val points: Int,
        val consumed: IntArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Match) return false
            return points == other.points && consumed.contentEquals(other.consumed)
        }
        override fun hashCode(): Int = 31 * points + consumed.contentHashCode()
    }
}
