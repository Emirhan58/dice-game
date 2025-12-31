package com.kiwixgames.dice.domain.model.game

import com.kiwixgames.dice.domain.enums.GameStatus
import com.kiwixgames.dice.domain.enums.TurnPhase

data class GameState(
    val status: GameStatus = GameStatus.IN_PROGRESS,
    val targetScore: Int,
    val activeSeat: Int = 0,     // 0 or 1
    val totalScores: IntArray = intArrayOf(0, 0),
    val turnScore: Int = 0,

    val remainingSlots: IntArray = intArrayOf(0,1,2,3,4,5), // dice slots
    val lastRoll: List<RolledDie>? = null,
    val phase: TurnPhase = TurnPhase.MUST_ROLL
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GameState

        if (targetScore != other.targetScore) return false
        if (activeSeat != other.activeSeat) return false
        if (turnScore != other.turnScore) return false
        if (status != other.status) return false
        if (!totalScores.contentEquals(other.totalScores)) return false
        if (!remainingSlots.contentEquals(other.remainingSlots)) return false
        if (lastRoll != other.lastRoll) return false
        if (phase != other.phase) return false

        return true
    }

    override fun hashCode(): Int {
        var result = targetScore
        result = 31 * result + activeSeat
        result = 31 * result + turnScore
        result = 31 * result + status.hashCode()
        result = 31 * result + totalScores.contentHashCode()
        result = 31 * result + remainingSlots.contentHashCode()
        result = 31 * result + (lastRoll?.hashCode() ?: 0)
        result = 31 * result + phase.hashCode()
        return result
    }
}