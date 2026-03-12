package com.kiwixgames.dice.services.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.kiwixgames.dice.domain.dtos.game.GameEvent
import com.kiwixgames.dice.domain.entities.Game
import com.kiwixgames.dice.domain.entities.User
import com.kiwixgames.dice.domain.enums.GameStatus
import com.kiwixgames.dice.domain.enums.TableStatus
import com.kiwixgames.dice.domain.enums.TurnPhase
import com.kiwixgames.dice.domain.model.game.GameState
import com.kiwixgames.dice.domain.model.game.RolledDie
import com.kiwixgames.dice.repositories.GameRepository
import com.kiwixgames.dice.repositories.TableRepository
import com.kiwixgames.dice.services.GameEventPublisher
import com.kiwixgames.dice.services.GamePlayService
import com.kiwixgames.dice.services.WalletService
import com.kiwixgames.dice.services.game.Kcd2ScoringMax
import jakarta.persistence.EntityNotFoundException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDateTime

@Service
class GamePlayServiceImpl(
    private val gameRepository: GameRepository,
    private val tableRepository: TableRepository,
    private val walletService: WalletService,
    private val objectMapper: ObjectMapper,
    private val publisher: GameEventPublisher
) : GamePlayService {

    private val rng = SecureRandom()

    @Transactional(readOnly = true)
    override fun getState(gameId: Long, me: User): GameState {
        val game = gameRepository.findById(gameId).orElseThrow {
            EntityNotFoundException("Game not found: $gameId")
        }
        requirePlayer(game, me)
        return readState(game)
    }

    @Transactional
    override fun roll(gameId: Long, me: User): GameState {
        val game = gameRepository.findById(gameId).orElseThrow {
            EntityNotFoundException("Game not found: $gameId")
        }
        val seat = requirePlayer(game, me)
        val state = readState(game)

        ensureInProgress(state)
        ensureActiveSeat(state, seat)

        if (state.phase == TurnPhase.MUST_KEEP_OR_BUST) {
            error("You must keep at least one scoring die before rolling again")
        }

        val remaining = state.remainingSlots
        if (remaining.isEmpty()) error("No dice remaining to roll")

        val roll = remaining.map { slot -> RolledDie(slot = slot, value = rollDie()) }

        val possible = Kcd2ScoringMax.scoreMax(roll.map { it.value }) > 0

        return if (!possible) {
            // BUST: turnScore=0, switch turn immediately
            val busted = state.copy(
                lastRoll = roll,
                phase = TurnPhase.MUST_KEEP_OR_BUST,
                lastActionAtEpochMs = now()
            )
            val next = switchTurn(busted, bust = true).copy(lastActionAtEpochMs = now())

            publisher.publish(gameId, GameEvent(
                type = "BUST",
                gameId = gameId,
                tableId = game.table.id!!,
                bySeat = seat,
                payload = mapOf("rolled" to roll)
            ))
            publisher.publish(gameId, GameEvent(
                type = "TURN_CHANGED",
                gameId = gameId,
                tableId = game.table.id!!,
                bySeat = next.activeSeat
            ))

            saveState(game, next)
        } else {
            val next = state.copy(
                lastRoll = roll,
                phase = TurnPhase.MUST_KEEP_OR_BUST,
                lastActionAtEpochMs = now()
            )

            publisher.publish(gameId, GameEvent(
                type = "ROLLED",
                gameId = gameId,
                tableId = game.table.id!!,
                bySeat = seat,
                payload = mapOf("roll" to roll)
            ))

            saveState(game, next)
        }
    }

    @Transactional
    override fun keep(gameId: Long, me: User, slots: List<Int>): GameState {
        val game = gameRepository.findById(gameId).orElseThrow {
            EntityNotFoundException("Game not found: $gameId")
        }
        val seat = requirePlayer(game, me)
        val state = readState(game)

        ensureInProgress(state)
        ensureActiveSeat(state, seat)

        val lastRoll = state.lastRoll ?: error("No roll to keep from")
        if (state.phase != TurnPhase.MUST_KEEP_OR_BUST) error("You can keep only right after a roll")
        if (slots.isEmpty()) error("slots cannot be empty")

        val rolledBySlot = lastRoll.associateBy { it.slot }
        val remainingSet = state.remainingSlots.toSet()

        slots.forEach { s ->
            if (!remainingSet.contains(s)) error("Slot $s is not in remaining dice")
            if (!rolledBySlot.containsKey(s)) error("Slot $s was not rolled")
        }

        val keptValues = slots.map { rolledBySlot[it]!!.value }
        val gained = Kcd2ScoringMax.scoreMax(keptValues)
        if (gained <= 0) error("Selected dice do not form a valid scoring combination")

        // Ensure every kept die contributes to scoring (no wasted dice)
        for (i in slots.indices) {
            val without = keptValues.toMutableList().apply { removeAt(i) }
            if (Kcd2ScoringMax.scoreMax(without) == gained) {
                val dieValue = keptValues[i]
                error("Die with value $dieValue at slot ${slots[i]} does not contribute to any scoring combination")
            }
        }

        val newRemaining = state.remainingSlots.filter { it !in slots }.toIntArray()
        val newTurnScore = state.turnScore + gained

        val hot = newRemaining.isEmpty()
        val afterKeep = state.copy(
            turnScore = newTurnScore,
            remainingSlots = if (hot) intArrayOf(0, 1, 2, 3, 4, 5) else newRemaining,
            lastRoll = null,
            phase = TurnPhase.CAN_ROLL_OR_BANK,
            lastActionAtEpochMs = now()
        )

        publisher.publish(gameId, GameEvent(
            type = "KEPT",
            gameId = gameId,
            tableId = game.table.id!!,
            bySeat = seat,
            payload = mapOf(
                "slots" to slots,
                "gained" to gained,
                "turnScore" to afterKeep.turnScore,
                "hotDice" to hot
            )
        ))

        return saveState(game, afterKeep)
    }

    @Transactional
    override fun bank(gameId: Long, me: User): GameState {
        val game = gameRepository.findById(gameId).orElseThrow {
            EntityNotFoundException("Game not found: $gameId")
        }
        val seat = requirePlayer(game, me)
        val state = readState(game)

        ensureInProgress(state)
        ensureActiveSeat(state, seat)

        if (state.turnScore <= 0) error("Nothing to bank")
        if (state.phase == TurnPhase.MUST_KEEP_OR_BUST) {
            error("You must keep or bust before banking (you have an unresolved roll)")
        }

        val totals = state.totalScores.copyOf()
        totals[seat] += state.turnScore

        publisher.publish(gameId, GameEvent(
            type = "BANKED",
            gameId = gameId,
            tableId = game.table.id!!,
            bySeat = seat,
            payload = mapOf("banked" to state.turnScore, "total" to totals[seat])
        ))

        if (totals[seat] >= state.targetScore) {
            val finished = state.copy(
                totalScores = totals,
                turnScore = 0,
                phase = TurnPhase.MUST_ROLL,
                lastRoll = null,
                lastActionAtEpochMs = now()
            )
            val final = finalizeGame(game, finished, winnerSeat = seat)
            return saveState(game, final)
        }


        val next = switchTurn(
            state.copy(
                totalScores = totals,
                turnScore = 0,
                remainingSlots = intArrayOf(0, 1, 2, 3, 4, 5),
                lastRoll = null,
                phase = TurnPhase.MUST_ROLL,
                lastActionAtEpochMs = now()
            ),
            bust = false
        ).copy(lastActionAtEpochMs = now())

        publisher.publish(gameId, GameEvent(
            type = "TURN_CHANGED",
            gameId = gameId,
            tableId = game.table.id!!,
            bySeat = next.activeSeat
        ))

        return saveState(game, next)
    }

    // ---------------- helpers ----------------

    private fun rollDie(): Int = rng.nextInt(6) + 1
    private fun now(): Long = System.currentTimeMillis()

    private fun ensureInProgress(state: GameState) {
        if (state.status != GameStatus.IN_PROGRESS) error("Game is not in progress")
    }

    private fun ensureActiveSeat(state: GameState, seat: Int) {
        if (state.activeSeat != seat) error("Not your turn")
    }

    private fun requirePlayer(game: Game, me: User): Int {
        val table = game.table
        val s0 = table.seat0?.id
        val s1 = table.seat1?.id
        val myId = me.id ?: error("User id is null")
        return when (myId) {
            s0 -> 0
            s1 -> 1
            else -> throw IllegalStateException("You are not a player in this game")
        }
    }

    private fun readState(game: Game): GameState {
        val json = game.stateJson
        return if (json.isNullOrBlank()) {
            GameState(
                status = GameStatus.IN_PROGRESS,
                targetScore = game.targetScore,
                activeSeat = 0,
                totalScores = intArrayOf(0, 0),
                turnScore = 0,
                remainingSlots = intArrayOf(0, 1, 2, 3, 4, 5),
                lastRoll = null,
                phase = TurnPhase.MUST_ROLL,
                lastActionAtEpochMs = now()
            )
        } else {
            objectMapper.readValue(json, GameState::class.java)
        }
    }

    private fun saveState(game: Game, state: GameState): GameState {
        game.stateJson = objectMapper.writeValueAsString(state)

        if (state.status == GameStatus.FINISHED) {
            game.status = GameStatus.FINISHED
            if (game.finishedAt == null) game.finishedAt = LocalDateTime.now()
        }

        try {
            gameRepository.save(game)
        } catch (e: OptimisticLockingFailureException) {
            throw IllegalStateException("Game state changed by another request. Retry.")
        }
        return state
    }

    private fun switchTurn(state: GameState, bust: Boolean): GameState {
        val nextSeat = 1 - state.activeSeat
        return state.copy(
            activeSeat = nextSeat,
            turnScore = 0,
            remainingSlots = intArrayOf(0, 1, 2, 3, 4, 5),
            lastRoll = if (bust) state.lastRoll else null,
            phase = TurnPhase.MUST_ROLL
        )
    }

    private fun finalizeGame(game: Game, state: GameState, winnerSeat: Int): GameState {
        game.winnerSeat = winnerSeat
        game.finishedAt = LocalDateTime.now()
        game.status = GameStatus.FINISHED

        val table = game.table
        table.status = TableStatus.FINISHED
        tableRepository.save(table)

        walletService.payoutOnce(table.id!!, winnerSeat)

        publisher.publish(game.id!!, GameEvent(
            type = "FINISHED",
            gameId = game.id!!,
            tableId = table.id!!,
            bySeat = winnerSeat,
            payload = mapOf("winnerSeat" to winnerSeat)
        ))

        return state.copy(status = GameStatus.FINISHED)
    }
}