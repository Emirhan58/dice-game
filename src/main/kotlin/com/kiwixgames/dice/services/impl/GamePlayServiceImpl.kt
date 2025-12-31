package com.kiwixgames.dice.services.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.kiwixgames.dice.domain.entities.Game
import com.kiwixgames.dice.domain.entities.User
import com.kiwixgames.dice.domain.enums.GameStatus
import com.kiwixgames.dice.domain.enums.TableStatus
import com.kiwixgames.dice.domain.enums.TurnPhase
import com.kiwixgames.dice.domain.enums.WagerLockStatus
import com.kiwixgames.dice.domain.model.game.GameState
import com.kiwixgames.dice.domain.model.game.RolledDie
import com.kiwixgames.dice.repositories.GameRepository
import com.kiwixgames.dice.repositories.TableRepository
import com.kiwixgames.dice.services.GamePlayService
import com.kiwixgames.dice.services.WalletService
import com.kiwixgames.dice.repositories.WagerLockRepository
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
    private val wagerLockRepository: WagerLockRepository,
    private val walletService: WalletService,
    private val objectMapper: ObjectMapper
) : GamePlayService {

    private val rng = SecureRandom()

    override fun getState(gameId: Long, me: User): GameState {
        val game = gameRepository.findById(gameId).orElseThrow { EntityNotFoundException("Game not found: $gameId") }
        requirePlayer(game, me)
        return readState(game)
    }

    @Transactional
    override fun roll(gameId: Long, me: User): GameState {
        val game = gameRepository.findById(gameId).orElseThrow { EntityNotFoundException("Game not found: $gameId") }
        val seat = requirePlayer(game, me)
        val state = readState(game)

        ensureInProgress(state)
        ensureActiveSeat(state, seat)
        if (state.phase == TurnPhase.MUST_KEEP_OR_BUST) error("You must keep at least one scoring die before rolling again")

        val remaining = state.remainingSlots
        if (remaining.isEmpty()) error("No dice remaining to roll")

        val roll = remaining.map { slot -> RolledDie(slot = slot, value = rollDie()) }

        // Bust check: any score possible from this roll?
        val values = roll.map { it.value }
        val possible = Kcd2ScoringMax.scoreMax(values) > 0
        if (!possible) {
            // BUST: turnScore = 0, switch turn
            val next = switchTurn(state.copy(lastRoll = roll, phase = TurnPhase.MUST_KEEP_OR_BUST), bust = true)
            return saveState(game, next)
        }

        val next = state.copy(lastRoll = roll, phase = TurnPhase.MUST_KEEP_OR_BUST)
        return saveState(game, next)
    }

    @Transactional
    override fun keep(gameId: Long, me: User, slots: List<Int>): GameState {
        val game = gameRepository.findById(gameId).orElseThrow { EntityNotFoundException("Game not found: $gameId") }
        val seat = requirePlayer(game, me)
        val state = readState(game)

        ensureInProgress(state)
        ensureActiveSeat(state, seat)

        val lastRoll = state.lastRoll ?: error("No roll to keep from")
        if (state.phase != TurnPhase.MUST_KEEP_OR_BUST) error("You can keep only right after a roll")
        if (slots.isEmpty()) error("slots cannot be empty")

        val rolledBySlot = lastRoll.associateBy { it.slot }
        val remainingSet = state.remainingSlots.toSet()

        // validate slots are from current roll & remaining
        slots.forEach { s ->
            if (!remainingSet.contains(s)) error("Slot $s is not in remaining dice")
            if (!rolledBySlot.containsKey(s)) error("Slot $s was not rolled")
        }

        val keptValues = slots.map { rolledBySlot[it]!!.value }
        val gained = Kcd2ScoringMax.scoreMax(keptValues)
        if (gained <= 0) error("Selected dice do not form a valid scoring combination")

        // remove kept slots from remaining
        val newRemaining = state.remainingSlots.filter { it !in slots }.toIntArray()

        val newTurnScore = state.turnScore + gained

        // HOT DICE: if no dice left, reset to 6
        val hot = newRemaining.isEmpty()
        val afterKeep = state.copy(
            turnScore = newTurnScore,
            remainingSlots = if (hot) intArrayOf(0,1,2,3,4,5) else newRemaining,
            lastRoll = null,
            phase = TurnPhase.CAN_ROLL_OR_BANK
        )

        // immediate win rule (your requirement)
        val maybeFinished = finishIfReachedTarget(game, afterKeep)

        return saveState(game, maybeFinished)
    }

    @Transactional
    override fun bank(gameId: Long, me: User): GameState {
        val game = gameRepository.findById(gameId).orElseThrow { EntityNotFoundException("Game not found: $gameId") }
        val seat = requirePlayer(game, me)
        val state = readState(game)

        ensureInProgress(state)
        ensureActiveSeat(state, seat)

        if (state.turnScore <= 0) error("Nothing to bank")
        if (state.phase == TurnPhase.MUST_KEEP_OR_BUST) error("You must keep or bust before banking (you have an unresolved roll)")

        val totals = state.totalScores.copyOf()
        totals[seat] += state.turnScore

        // win check on bank as well (still consistent)
        if (totals[seat] >= state.targetScore) {
            val finished = state.copy(
                totalScores = totals,
                turnScore = 0,
                phase = TurnPhase.MUST_ROLL
            )
            val final = finalizeGame(game, finished, winnerSeat = seat)
            return saveState(game, final)
        }

        val next = switchTurn(
            state.copy(
                totalScores = totals,
                turnScore = 0,
                remainingSlots = intArrayOf(0,1,2,3,4,5),
                lastRoll = null,
                phase = TurnPhase.MUST_ROLL
            ),
            bust = false
        )
        return saveState(game, next)
    }

    // ---------------- helpers ----------------

    private fun rollDie(): Int = rng.nextInt(6) + 1

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
            // default initial state
            GameState(
                status = GameStatus.IN_PROGRESS,
                targetScore = game.targetScore,
                activeSeat = 0,
                totalScores = intArrayOf(0, 0),
                turnScore = 0,
                remainingSlots = intArrayOf(0,1,2,3,4,5),
                lastRoll = null,
                phase = TurnPhase.MUST_ROLL
            )
        } else {
            objectMapper.readValue(json, GameState::class.java)
        }
    }

    private fun saveState(game: Game, state: GameState): GameState {
        // persist state + metadata
        game.stateJson = objectMapper.writeValueAsString(state)

        // also keep status info at entity level if finished
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
        // bust -> turnScore reset & switch
        val nextSeat = 1 - state.activeSeat
        return state.copy(
            activeSeat = nextSeat,
            turnScore = if (bust) 0 else state.turnScore,
            remainingSlots = intArrayOf(0,1,2,3,4,5),
            lastRoll = null,
            phase = TurnPhase.MUST_ROLL
        )
    }

    private fun finishIfReachedTarget(game: Game, state: GameState): GameState {
        val seat = state.activeSeat
        val currentTotal = state.totalScores[seat]
        if (currentTotal + state.turnScore >= state.targetScore) {
            val totals = state.totalScores.copyOf()
            totals[seat] = currentTotal + state.turnScore

            val finished = state.copy(
                totalScores = totals,
                turnScore = 0,
                status = GameStatus.FINISHED,
                phase = TurnPhase.MUST_ROLL,
                lastRoll = null
            )
            return finalizeGame(game, finished, winnerSeat = seat)
        }
        return state
    }

    private fun finalizeGame(game: Game, state: GameState, winnerSeat: Int): GameState {
        // mark game/table
        game.winnerSeat = winnerSeat
        game.finishedAt = LocalDateTime.now()
        game.status = GameStatus.FINISHED

        val table = game.table
        table.status = TableStatus.FINISHED
        tableRepository.save(table)

        // payout (idempotent)
        payoutOnce(table.id!!, winnerSeat)

        return state.copy(status = GameStatus.FINISHED)
    }

    private fun payoutOnce(tableId: Long, winnerSeat: Int) {
        val locks = wagerLockRepository.findAllByTableId(tableId)
        if (locks.isEmpty()) return

        // if already paid out, skip
        if (locks.all { it.status == WagerLockStatus.PAID_OUT }) return

        val table = tableRepository.findById(tableId).orElseThrow { EntityNotFoundException("Table not found: $tableId") }
        val winnerUser = if (winnerSeat == 0) table.seat0 else table.seat1
        val winner = winnerUser ?: error("Winner user is null")

        val stake = table.stakeGold
        val payout = stake * 2

        // mark locks
        locks.forEach {
            if (it.status == WagerLockStatus.LOCKED) it.status = WagerLockStatus.PAID_OUT
        }
        wagerLockRepository.saveAll(locks)

        walletService.payoutWinner(winner, tableId, payout)
    }
}
