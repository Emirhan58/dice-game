package com.kiwixgames.dice.services.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.kiwixgames.dice.domain.entities.Game
import com.kiwixgames.dice.domain.entities.GameTable
import com.kiwixgames.dice.domain.model.game.GameState
import com.kiwixgames.dice.repositories.GameRepository
import com.kiwixgames.dice.services.GameService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom

@Service
class GameServiceImpl(
    private val gameRepository: GameRepository,
    private val objectMapper: ObjectMapper
) : GameService {

    private val rng = SecureRandom()

    @Transactional
    override fun startGame(table: GameTable): Game {
        val existing = gameRepository.findByTableId(table.id!!)
        if (existing != null) return existing

        val firstSeat = rng.nextInt(2)
        val initial = GameState(targetScore = table.targetScore, activeSeat = firstSeat)

        return gameRepository.save(
            Game(
                table = table,
                targetScore = table.targetScore,
                stateJson = objectMapper.writeValueAsString(initial)
            )
        )
    }
}
