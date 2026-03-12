package com.kiwixgames.dice.services.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Kcd2ScoringMaxTest {

    // --- Singles ---

    @Test
    fun `single 1 scores 100`() {
        assertEquals(100, Kcd2ScoringMax.scoreMax(listOf(1)))
    }

    @Test
    fun `single 5 scores 50`() {
        assertEquals(50, Kcd2ScoringMax.scoreMax(listOf(5)))
    }

    @Test
    fun `non-scoring single die scores 0`() {
        assertEquals(0, Kcd2ScoringMax.scoreMax(listOf(2)))
        assertEquals(0, Kcd2ScoringMax.scoreMax(listOf(3)))
        assertEquals(0, Kcd2ScoringMax.scoreMax(listOf(4)))
        assertEquals(0, Kcd2ScoringMax.scoreMax(listOf(6)))
    }

    @Test
    fun `two 1s score 200`() {
        assertEquals(200, Kcd2ScoringMax.scoreMax(listOf(1, 1)))
    }

    @Test
    fun `1 and 5 score 150`() {
        assertEquals(150, Kcd2ScoringMax.scoreMax(listOf(1, 5)))
    }

    @Test
    fun `no scoring dice returns 0`() {
        assertEquals(0, Kcd2ScoringMax.scoreMax(listOf(2, 3, 4, 6)))
        assertEquals(0, Kcd2ScoringMax.scoreMax(listOf(2, 3)))
    }

    @Test
    fun `empty list returns 0`() {
        assertEquals(0, Kcd2ScoringMax.scoreMax(emptyList()))
    }

    // --- Three of a kind ---

    @Test
    fun `three 1s score 1000`() {
        assertEquals(1000, Kcd2ScoringMax.scoreMax(listOf(1, 1, 1)))
    }

    @Test
    fun `three 2s score 200`() {
        assertEquals(200, Kcd2ScoringMax.scoreMax(listOf(2, 2, 2)))
    }

    @Test
    fun `three 3s score 300`() {
        assertEquals(300, Kcd2ScoringMax.scoreMax(listOf(3, 3, 3)))
    }

    @Test
    fun `three 4s score 400`() {
        assertEquals(400, Kcd2ScoringMax.scoreMax(listOf(4, 4, 4)))
    }

    @Test
    fun `three 5s score 500`() {
        assertEquals(500, Kcd2ScoringMax.scoreMax(listOf(5, 5, 5)))
    }

    @Test
    fun `three 6s score 600`() {
        assertEquals(600, Kcd2ScoringMax.scoreMax(listOf(6, 6, 6)))
    }

    @Test
    fun `three 1s plus single 5 scores 1050`() {
        assertEquals(1050, Kcd2ScoringMax.scoreMax(listOf(1, 1, 1, 5)))
    }

    @Test
    fun `mixed - three 2s plus 1 and 5 scores 350`() {
        assertEquals(350, Kcd2ScoringMax.scoreMax(listOf(2, 2, 2, 1, 5)))
    }

    // --- Doubling (4, 5, 6 of a kind) ---

    @Test
    fun `four 1s scores 2000 - double of three 1s`() {
        assertEquals(2000, Kcd2ScoringMax.scoreMax(listOf(1, 1, 1, 1)))
    }

    @Test
    fun `five 1s scores 4000`() {
        assertEquals(4000, Kcd2ScoringMax.scoreMax(listOf(1, 1, 1, 1, 1)))
    }

    @Test
    fun `six 1s scores 8000`() {
        assertEquals(8000, Kcd2ScoringMax.scoreMax(listOf(1, 1, 1, 1, 1, 1)))
    }

    @Test
    fun `four 3s scores 600`() {
        assertEquals(600, Kcd2ScoringMax.scoreMax(listOf(3, 3, 3, 3)))
    }

    @Test
    fun `five 5s scores 2000`() {
        assertEquals(2000, Kcd2ScoringMax.scoreMax(listOf(5, 5, 5, 5, 5)))
    }

    @Test
    fun `six 6s scores 4800`() {
        assertEquals(4800, Kcd2ScoringMax.scoreMax(listOf(6, 6, 6, 6, 6, 6)))
    }

    @Test
    fun `four 2s scores 400`() {
        assertEquals(400, Kcd2ScoringMax.scoreMax(listOf(2, 2, 2, 2)))
    }

    @Test
    fun `five 3s scores 1200`() {
        assertEquals(1200, Kcd2ScoringMax.scoreMax(listOf(3, 3, 3, 3, 3)))
    }

    // --- Straight ---

    @Test
    fun `straight 1-2-3-4-5-6 scores 3000`() {
        assertEquals(3000, Kcd2ScoringMax.scoreMax(listOf(1, 2, 3, 4, 5, 6)))
    }

    // --- Three Pairs ---

    @Test
    fun `three pairs score 1500`() {
        assertEquals(1500, Kcd2ScoringMax.scoreMax(listOf(2, 2, 3, 3, 4, 4)))
        assertEquals(1500, Kcd2ScoringMax.scoreMax(listOf(1, 1, 3, 3, 6, 6)))
    }

    @Test
    fun `four of a kind plus pair scores 1500 as three pairs`() {
        assertEquals(1500, Kcd2ScoringMax.scoreMax(listOf(2, 2, 2, 2, 3, 3)))
    }

    @Test
    fun `three pairs with 1s and 5s - 1500 beats individual scoring`() {
        // 1,1,5,5,3,3 -> 3 pairs = 1500 vs singles (200+100+300=600)
        assertEquals(1500, Kcd2ScoringMax.scoreMax(listOf(1, 1, 5, 5, 3, 3)))
    }

    // --- Two Triplets ---

    @Test
    fun `two triplets score 2500`() {
        assertEquals(2500, Kcd2ScoringMax.scoreMax(listOf(2, 2, 2, 4, 4, 4)))
        assertEquals(2500, Kcd2ScoringMax.scoreMax(listOf(3, 3, 3, 6, 6, 6)))
    }

    @Test
    fun `two triplets with 1s - 2500 beats triple-1s plus triple-other`() {
        // 1,1,1,3,3,3 -> two triplets = 2500 vs (1000 + 300 = 1300)
        assertEquals(2500, Kcd2ScoringMax.scoreMax(listOf(1, 1, 1, 3, 3, 3)))
    }

    // --- Edge cases: engine picks max ---

    @Test
    fun `four 5s plus 1 - doubling beats three plus singles`() {
        // four 5s = 1000, plus single 1 = 100 → 1100
        // vs three 5s (500) + single 5 (50) + single 1 (100) = 650
        assertEquals(1100, Kcd2ScoringMax.scoreMax(listOf(5, 5, 5, 5, 1)))
    }

    @Test
    fun `four 1s plus single 5 scores 2050`() {
        // four 1s = 2000 + single 5 = 50 → 2050
        assertEquals(2050, Kcd2ScoringMax.scoreMax(listOf(1, 1, 1, 1, 5)))
    }
}
