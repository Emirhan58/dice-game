package com.kiwixgames.dice.services

import com.kiwixgames.dice.domain.entities.Game
import com.kiwixgames.dice.domain.entities.User
import com.kiwixgames.dice.domain.model.game.GameState

interface GamePlayService {
    fun getState(gameId: Long, me: User): GameState
    fun roll(gameId: Long, me: User): GameState
    fun keep(gameId: Long, me: User, slots: List<Int>): GameState
    fun bank(gameId: Long, me: User): GameState
    fun forfeit(gameId: Long, me: User, reason: String = "VOLUNTARY"): GameState
}
