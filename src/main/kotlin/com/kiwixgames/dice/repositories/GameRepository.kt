package com.kiwixgames.dice.repositories

import com.kiwixgames.dice.domain.entities.Game
import com.kiwixgames.dice.domain.enums.GameStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface GameRepository : JpaRepository<Game, Long> {
    fun findByTableId(tableId: Long): Game?
    fun findAllByStatus(status: GameStatus): List<Game>

    @Query("""
        SELECT g FROM Game g
        JOIN FETCH g.table t
        LEFT JOIN FETCH t.seat0
        LEFT JOIN FETCH t.seat1
        WHERE g.status = :status
    """)
    fun findAllByStatusWithPlayers(status: GameStatus): List<Game>

    @Query("""
        SELECT g FROM Game g
        JOIN FETCH g.table t
        LEFT JOIN FETCH t.seat0
        LEFT JOIN FETCH t.seat1
        WHERE g.id = :id
    """)
    fun findByIdWithPlayers(id: Long): Game?

    @Query("""
        SELECT g FROM Game g
        JOIN FETCH g.table t
        LEFT JOIN FETCH t.seat0
        LEFT JOIN FETCH t.seat1
        WHERE g.status = :status
        AND (t.seat0.id = :userId OR t.seat1.id = :userId)
    """)
    fun findAllByStatusAndUserId(status: GameStatus, userId: Long): List<Game>
}
