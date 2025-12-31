package com.kiwixgames.dice.services.game

object Kcd2ScoringMax {

    fun scoreMax(values: List<Int>): Int {
        if (values.isEmpty()) return 0
        val counts = IntArray(7)
        values.forEach { v -> counts[v]++ }
        val memo = HashMap<String, Int>()
        return dfs(counts, memo)
    }

    private fun key(c: IntArray): String =
        "${c[1]}-${c[2]}-${c[3]}-${c[4]}-${c[5]}-${c[6]}"

    private fun dfs(c: IntArray, memo: MutableMap<String, Int>): Int {
        val k = key(c)
        memo[k]?.let { return it }

        var best = 0

        // --- Singles ---
        if (c[1] > 0) {
            c[1]--
            best = maxOf(best, 100 + dfs(c, memo))
            c[1]++
        }
        if (c[5] > 0) {
            c[5]--
            best = maxOf(best, 50 + dfs(c, memo))
            c[5]++
        }

        // --- Straights ---
        fun takeStraight(range: IntRange, points: Int) {
            if (range.all { c[it] > 0 }) {
                range.forEach { c[it]-- }
                best = maxOf(best, points + dfs(c, memo))
                range.forEach { c[it]++ }
            }
        }
        takeStraight(1..5, 500)
        takeStraight(2..6, 750)
        takeStraight(1..6, 1500)

        // --- N-of-a-kind (3+) with doubling ---
        for (v in 1..6) {
            val cnt = c[v]
            if (cnt >= 3) {
                val base = if (v == 1) 1000 else v * 100

                // k = 3..min(6,cnt)
                for (kCount in 3..minOf(6, cnt)) {
                    val multiplier = 1 shl (kCount - 3)  // 3->1x, 4->2x, 5->4x, 6->8x
                    repeat(kCount) { c[v]-- }
                    best = maxOf(best, base * multiplier + dfs(c, memo))
                    repeat(kCount) { c[v]++ }
                }
            }
        }

        memo[k] = best
        return best
    }
}
