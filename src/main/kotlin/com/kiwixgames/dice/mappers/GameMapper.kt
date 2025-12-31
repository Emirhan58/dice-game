package com.kiwixgames.dice.mappers

import com.kiwixgames.dice.domain.dtos.game.GameStateResponse
import com.kiwixgames.dice.domain.dtos.game.RolledDieDto
import com.kiwixgames.dice.domain.entities.Game
import com.kiwixgames.dice.domain.model.game.GameState

object GameMapper {
    fun toDto(game: Game, state: GameState, mySeat: Int): GameStateResponse {
        return GameStateResponse(
            gameId = game.id!!,
            tableId = game.table.id!!,
            status = state.status.name,
            targetScore = state.targetScore,
            activeSeat = state.activeSeat,
            mySeat = mySeat,
            totalScores = listOf(state.totalScores[0], state.totalScores[1]),
            turnScore = state.turnScore,
            remainingDiceCount = state.remainingSlots.size,
            phase = state.phase.name,
            lastRoll = state.lastRoll?.map { RolledDieDto(it.slot, it.value) }
        )
    }
}