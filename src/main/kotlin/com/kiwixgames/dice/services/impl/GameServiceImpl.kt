package com.kiwixgames.dice.services.impl

import com.kiwixgames.dice.domain.entities.Game
import com.kiwixgames.dice.domain.entities.GameTable
import com.kiwixgames.dice.repositories.GameRepository
import com.kiwixgames.dice.services.GameService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GameServiceImpl(
    private val gameRepository: GameRepository
) : GameService {

    @Transactional
    override fun startGame(table: GameTable): Game {
        val existing = gameRepository.findByTableId(table.id!!)
        if (existing != null) return existing

        return gameRepository.save(
            Game(
                table = table,
                targetScore = table.targetScore
            )
        )
    }
}
