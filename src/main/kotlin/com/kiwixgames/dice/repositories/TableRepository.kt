package com.kiwixgames.dice.repositories

import com.kiwixgames.dice.domain.entities.GameTable
import com.kiwixgames.dice.domain.enums.TableStatus
import org.springframework.data.jpa.repository.JpaRepository

interface TableRepository : JpaRepository<GameTable, Long> {
    fun findAllByStatus(status: TableStatus): List<GameTable>
}
