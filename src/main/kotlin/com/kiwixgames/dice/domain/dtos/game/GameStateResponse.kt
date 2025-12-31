package com.kiwixgames.dice.domain.dtos.game

data class GameStateResponse(
    val gameId: Long,
    val tableId: Long,
    val status: String,
    val targetScore: Int,
    val activeSeat: Int,
    val mySeat: Int,
    val totalScores: List<Int>,
    val turnScore: Int,
    val remainingDiceCount: Int,
    val phase: String,
    val lastRoll: List<RolledDieDto>?
)