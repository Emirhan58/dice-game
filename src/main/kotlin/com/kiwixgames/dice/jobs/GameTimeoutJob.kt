package com.kiwixgames.dice.jobs

import com.fasterxml.jackson.databind.ObjectMapper
import com.kiwixgames.dice.domain.enums.GameStatus
import com.kiwixgames.dice.repositories.GameRepository
import com.kiwixgames.dice.services.GameEventPublisher
import com.kiwixgames.dice.services.WalletService
import com.kiwixgames.dice.repositories.TableRepository
import com.kiwixgames.dice.repositories.WagerLockRepository
import com.kiwixgames.dice.domain.dtos.game.GameEvent
import com.kiwixgames.dice.domain.enums.WagerLockStatus
import com.kiwixgames.dice.domain.model.game.GameState
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Component
class GameTimeoutJob(
    private val gameRepository: GameRepository,
    private val tableRepository: TableRepository,
    private val wagerLockRepository: WagerLockRepository,
    private val walletService: WalletService,
    private val publisher: GameEventPublisher,
    private val objectMapper: ObjectMapper
) {
    private val timeoutMs = 60_000L

    @Scheduled(fixedDelay = 5_000L)
    @Transactional
    fun checkTimeouts() {
        val games = gameRepository.findAll() // "IN_PROGRESS"
        val now = System.currentTimeMillis()

        for (g in games) {
            if (g.status != GameStatus.IN_PROGRESS) continue
            val json = g.stateJson ?: continue
            val state = objectMapper.readValue(json, GameState::class.java)

            if (now - state.lastActionAtEpochMs < timeoutMs) continue

            // activeSeat timeout -> other wins
            val loserSeat = state.activeSeat
            val winnerSeat = 1 - loserSeat

            // mark finished
            g.status = GameStatus.FINISHED
            g.winnerSeat = winnerSeat
            g.finishedAt = LocalDateTime.now()

            // update state
            val finishedState = state.copy(
                status = GameStatus.FINISHED
            )
            g.stateJson = objectMapper.writeValueAsString(finishedState)
            gameRepository.save(g)

            // table finish + payout once (idempotent)
            val table = g.table
            table.status = com.kiwixgames.dice.domain.enums.TableStatus.FINISHED
            tableRepository.save(table)

            payoutOnce(table.id!!, winnerSeat)

            publisher.publish(g.id!!, GameEvent(
                type = "FORFEIT",
                gameId = g.id!!,
                tableId = table.id!!,
                bySeat = loserSeat,
                payload = mapOf("winnerSeat" to winnerSeat, "reason" to "TIMEOUT")
            ))
        }
    }

    private fun payoutOnce(tableId: Long, winnerSeat: Int) {
        val locks = wagerLockRepository.findAllByTableId(tableId)
        if (locks.isEmpty()) return
        if (locks.all { it.status == WagerLockStatus.PAID_OUT }) return

        val table = tableRepository.findById(tableId).orElseThrow()
        val winner = if (winnerSeat == 0) table.seat0!! else table.seat1!!
        val payout = table.stakeGold * 2

        locks.forEach { if (it.status == WagerLockStatus.LOCKED) it.status = WagerLockStatus.PAID_OUT }
        wagerLockRepository.saveAll(locks)

        walletService.payoutWinner(winner, tableId, payout)
    }
}
