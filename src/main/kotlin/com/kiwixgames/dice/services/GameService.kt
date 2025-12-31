package com.kiwixgames.dice.services

import com.kiwixgames.dice.domain.entities.GameTable
import com.kiwixgames.dice.domain.entities.Game

interface GameService {
    fun startGame(table: GameTable): Game
}
