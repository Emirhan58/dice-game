package com.kiwixgames.dice.services.impl

import com.kiwixgames.dice.domain.dtos.game.GameEvent
import com.kiwixgames.dice.services.GameEventPublisher
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

@Service
class GameEventPublisherImpl(
    private val messaging: SimpMessagingTemplate
) : GameEventPublisher {

    override fun publish(gameId: Long, event: GameEvent) {
        messaging.convertAndSend("/topic/games/$gameId", event)
    }
}
