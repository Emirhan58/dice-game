package com.kiwixgames.dice.domain.dtos.game

data class GameEvent(
    val type: String,          // ROLLED / KEPT / BANKED / BUST / FINISHED / TURN_CHANGED / FORFEIT
    val gameId: Long,
    val tableId: Long,
    val bySeat: Int?,
    val payload: Any? = null
)
