package com.kiwixgames.dice.repositories

import com.kiwixgames.dice.domain.entities.Game
import org.springframework.data.jpa.repository.JpaRepository

interface GameRepository : JpaRepository<Game, Long> {
    fun findByTableId(tableId: Long): Game?
    fun findAllByStatus(status: com.kiwixgames.dice.domain.enums.GameStatus): List<Game>
}
