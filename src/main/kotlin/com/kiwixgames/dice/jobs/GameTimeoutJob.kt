package com.kiwixgames.dice.jobs

import com.fasterxml.jackson.databind.ObjectMapper
import com.kiwixgames.dice.domain.dtos.game.GameEvent
import com.kiwixgames.dice.domain.enums.GameStatus
import com.kiwixgames.dice.domain.enums.TableStatus
import com.kiwixgames.dice.domain.enums.WagerLockStatus
import com.kiwixgames.dice.domain.model.game.GameState
import com.kiwixgames.dice.repositories.GameRepository
import com.kiwixgames.dice.repositories.TableRepository
import com.kiwixgames.dice.repositories.UserRepository
import com.kiwixgames.dice.repositories.WagerLockRepository
import com.kiwixgames.dice.services.GameEventPublisher
import com.kiwixgames.dice.services.GamePlayService
import com.kiwixgames.dice.services.PlayerPresenceService
import com.kiwixgames.dice.services.WalletService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

@Component
class GameTimeoutJob(
    private val gameRepository: GameRepository,
    private val tableRepository: TableRepository,
    private val wagerLockRepository: WagerLockRepository,
    private val userRepository: UserRepository,
    private val walletService: WalletService,
    private val publisher: GameEventPublisher,
    private val objectMapper: ObjectMapper,
    private val txTemplate: TransactionTemplate,
    private val playerPresenceService: PlayerPresenceService,
    private val gamePlayService: GamePlayService
) {
    private val log = LoggerFactory.getLogger(GameTimeoutJob::class.java)

    private val gameTimeoutMs = 6_000_000L           // 100 minutes — absolute game timeout
    private val presenceTimeoutMs = 60_000L           // 60 seconds — player ping timeout
    private val disconnectGraceMs = 30_000L           // 30 seconds — WebSocket disconnect grace period
    private val waitingTimeoutMs = 1_800_000L         // 30 minutes — waiting table timeout

    /** Tracks which disconnects we've already notified about (gameId:userId) */
    private val notifiedDisconnects = ConcurrentHashMap.newKeySet<String>()

    /**
     * Unified connectivity check — handles both WebSocket disconnects (grace period)
     * and ping-based absence detection in a single pass.
     *
     * Priority: WS disconnect > ping timeout (a disconnected player also stops pinging,
     * so we skip ping checks for players already tracked by disconnect grace).
     */
    @Scheduled(fixedDelay = 5_000L)
    fun checkPlayerConnectivity() {
        val games = gameRepository.findAllByStatusWithPlayers(GameStatus.IN_PROGRESS)

        for (g in games) {
            val table = g.table
            val seat0Id = table.seat0?.id ?: continue
            val seat1Id = table.seat1?.id ?: continue
            val gameId = g.id!!

            val players = listOf(seat0Id to 0, seat1Id to 1)

            // --- Phase 1: Disconnect events (notify + reconnect) ---
            for ((userId, seat) in players) {
                val key = "$gameId:$userId"

                if (playerPresenceService.isDisconnected(gameId, userId)) {
                    if (notifiedDisconnects.add(key)) {
                        publisher.publish(gameId, GameEvent(
                            type = "PLAYER_DISCONNECTED",
                            gameId = gameId,
                            tableId = table.id!!,
                            bySeat = seat,
                            payload = mapOf("seat" to seat, "graceMs" to disconnectGraceMs)
                        ))
                        log.info("PLAYER_DISCONNECTED published: gameId=$gameId, userId=$userId")
                    }
                } else if (notifiedDisconnects.remove(key)) {
                    publisher.publish(gameId, GameEvent(
                        type = "PLAYER_RECONNECTED",
                        gameId = gameId,
                        tableId = table.id!!,
                        bySeat = seat,
                        payload = mapOf("seat" to seat)
                    ))
                    log.info("PLAYER_RECONNECTED published: gameId=$gameId, userId=$userId")
                }
            }

            // --- Phase 2: Determine if someone should be forfeited ---
            val forfeitUserId = findForfeitCandidate(gameId, seat0Id, seat1Id)
            if (forfeitUserId != null) {
                try {
                    val user = userRepository.findById(forfeitUserId).orElse(null) ?: continue
                    val reason = if (playerPresenceService.isDisconnected(gameId, forfeitUserId)) "DISCONNECT" else "PING_TIMEOUT"
                    gamePlayService.forfeit(gameId, user, reason)
                    cleanupGame(gameId)
                    log.info("Player userId=$forfeitUserId forfeited gameId=$gameId (reason=$reason)")
                } catch (e: Exception) {
                    log.error("Failed to forfeit gameId=$gameId for userId=$forfeitUserId: ${e.message}", e)
                }
            }
        }
    }

    /**
     * Returns the userId that should be forfeited, or null.
     * Disconnect grace (30s) is checked first, then ping absence (60s).
     * Only forfeits if exactly one player is offline.
     */
    private fun findForfeitCandidate(gameId: Long, seat0Id: Long, seat1Id: Long): Long? {
        val seat0Dc = playerPresenceService.isDisconnectedBeyondGrace(gameId, seat0Id, disconnectGraceMs)
        val seat1Dc = playerPresenceService.isDisconnectedBeyondGrace(gameId, seat1Id, disconnectGraceMs)

        // Disconnect grace expired — takes priority
        if (seat0Dc && !seat1Dc) return seat0Id
        if (seat1Dc && !seat0Dc) return seat1Id
        if (seat0Dc && seat1Dc) return null // both disconnected, don't forfeit

        // Skip ping check for players already in disconnect grace (they'll stop pinging too)
        val seat0Absent = !playerPresenceService.isDisconnected(gameId, seat0Id)
                && playerPresenceService.isAbsent(gameId, seat0Id, presenceTimeoutMs)
        val seat1Absent = !playerPresenceService.isDisconnected(gameId, seat1Id)
                && playerPresenceService.isAbsent(gameId, seat1Id, presenceTimeoutMs)

        if (seat0Absent && !seat1Absent) return seat0Id
        if (seat1Absent && !seat0Absent) return seat1Id
        return null
    }

    private fun cleanupGame(gameId: Long) {
        playerPresenceService.remove(gameId)
        notifiedDisconnects.removeIf { it.startsWith("$gameId:") }
    }

    /**
     * Absolute game timeout — if no action for 100 minutes, active player forfeits.
     */
    @Scheduled(fixedDelay = 30_000L)
    fun checkGameTimeouts() {
        val games = gameRepository.findAllByStatusWithPlayers(GameStatus.IN_PROGRESS)
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
                        payload = mapOf("winnerSeat" to winnerSeat, "loserSeat" to loserSeat, "reason" to "TIMEOUT")
                    ))

                    cleanupGame(game.id!!)
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
