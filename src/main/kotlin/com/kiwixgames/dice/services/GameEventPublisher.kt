package com.kiwixgames.dice.services

import com.kiwixgames.dice.domain.dtos.game.GameEvent

interface GameEventPublisher {
    fun publish(gameId: Long, event: GameEvent)
}