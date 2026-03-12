package com.kiwixgames.dice.jobs

import com.fasterxml.jackson.databind.ObjectMapper
import com.kiwixgames.dice.domain.enums.GameStatus
import com.kiwixgames.dice.domain.enums.TableStatus
import com.kiwixgames.dice.domain.enums.WagerLockStatus
import com.kiwixgames.dice.repositories.GameRepository
import com.kiwixgames.dice.repositories.TableRepository
import com.kiwixgames.dice.repositories.WagerLockRepository
import com.kiwixgames.dice.services.GameEventPublisher
import com.kiwixgames.dice.services.WalletService
import com.kiwixgames.dice.domain.dtos.game.GameEvent
import com.kiwixgames.dice.domain.model.game.GameState
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime

@Component
class GameTimeoutJob(
    private val gameRepository: GameRepository,
    private val tableRepository: TableRepository,
    private val wagerLockRepository: WagerLockRepository,
    private val walletService: WalletService,
    private val publisher: GameEventPublisher,
    private val objectMapper: ObjectMapper,
    private val txTemplate: TransactionTemplate
) {
    private val log = LoggerFactory.getLogger(GameTimeoutJob::class.java)

    private val gameTimeoutMs = 6_000_000L      // 100 minutes
    private val waitingTimeoutMs = 1_800_000L    // 30 minutes

    @Scheduled(fixedDelay = 5_000L)
    fun checkGameTimeouts() {
        val games = gameRepository.findAllByStatus(GameStatus.IN_PROGRESS)
        val now = System.currentTimeMillis()

        for (g in games) {
            val json = g.stateJson ?: continue
            val state = objectMapper.readValue(json, GameState::class.java)
            if (now - state.lastActionAtEpochMs < gameTimeoutMs) continue

            try {
                txTemplate.execute {
                    val game = gameRepository.findById(g.id!!).orElse(null) ?: return@execute
                    if (game.status != GameStatus.IN_PROGRESS) return@execute

                    val currentState = objectMapper.readValue(game.stateJson, GameState::class.java)
                    val loserSeat = currentState.activeSeat
                    val winnerSeat = 1 - loserSeat

                    game.status = GameStatus.FINISHED
                    game.winnerSeat = winnerSeat
                    game.finishedAt = LocalDateTime.now()

                    val finishedState = currentState.copy(status = GameStatus.FINISHED)
                    game.stateJson = objectMapper.writeValueAsString(finishedState)
                    gameRepository.save(game)

                    val table = game.table
                    table.status = TableStatus.FINISHED
                    tableRepository.save(table)

                    walletService.payoutOnce(table.id!!, winnerSeat)

                    publisher.publish(game.id!!, GameEvent(
                        type = "FORFEIT",
                        gameId = game.id!!,
                        tableId = table.id!!,
                        bySeat = loserSeat,
                        payload = mapOf("winnerSeat" to winnerSeat, "reason" to "TIMEOUT")
                    ))
                }
            } catch (e: Exception) {
                log.error("Failed to process game timeout for gameId=${g.id}: ${e.message}", e)
            }
        }
    }

    @Scheduled(fixedDelay = 30_000L)
    fun checkWaitingTableTimeouts() {
        val tables = tableRepository.findAllByStatus(TableStatus.WAITING)
        val now = System.currentTimeMillis()

        for (t in tables) {
            val createdAt = t.createdAt ?: continue
            val createdMs = createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (now - createdMs < waitingTimeoutMs) continue

            try {
                txTemplate.execute {
                    val table = tableRepository.findById(t.id!!).orElse(null) ?: return@execute
                    if (table.status != TableStatus.WAITING) return@execute

                    table.status = TableStatus.CANCELLED
                    tableRepository.save(table)

                    val locks = wagerLockRepository.findAllByTableId(table.id!!)
                    locks.forEach { lock ->
                        if (lock.status == WagerLockStatus.LOCKED) {
                            walletService.refundWager(lock.user, table.id!!, lock.stakeGold)
                            lock.status = WagerLockStatus.REFUNDED
                            wagerLockRepository.save(lock)
                        }
                    }
                }
            } catch (e: Exception) {
                log.error("Failed to process waiting table timeout for tableId=${t.id}: ${e.message}", e)
            }
        }
    }
}
