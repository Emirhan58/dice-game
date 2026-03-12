package com.kiwixgames.dice.services.game

/**
 * Farkle scoring engine.
 *
 * Uses DFS + memoization to find the combination of [ScoringRule]s
 * that maximizes the total score for a given set of dice.
 *
 * Rules are pluggable — see [FarkleScoringRules] to add or remove rules.
 */
object Kcd2ScoringMax {

    private val rules: List<ScoringRule> = FarkleScoringRules.defaultRules

    fun scoreMax(values: List<Int>): Int {
        if (values.isEmpty()) return 0
        val counts = IntArray(7)
        values.forEach { v -> counts[v]++ }
        val memo = HashMap<String, Int>()
        return dfs(counts, memo)
    }

    private fun key(c: IntArray): String =
        "${c[1]}-${c[2]}-${c[3]}-${c[4]}-${c[5]}-${c[6]}"

    private fun dfs(counts: IntArray, memo: MutableMap<String, Int>): Int {
        val k = key(counts)
        memo[k]?.let { return it }

        var best = 0
        for (rule in rules) {
            for (match in rule.matches(counts)) {
                best = maxOf(best, match.points + dfs(match.consumed, memo))
            }
        }

        memo[k] = best
        return best
    }
}
