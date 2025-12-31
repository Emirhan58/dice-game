package com.kiwixgames.dice.services.impl

import com.kiwixgames.dice.domain.dtos.table.CreateTableRequest
import com.kiwixgames.dice.domain.entities.GameTable
import com.kiwixgames.dice.domain.entities.User
import com.kiwixgames.dice.domain.entities.WagerLock
import com.kiwixgames.dice.domain.enums.TableStatus
import com.kiwixgames.dice.domain.enums.WagerLockStatus
import com.kiwixgames.dice.repositories.TableRepository
import com.kiwixgames.dice.repositories.WagerLockRepository
import com.kiwixgames.dice.services.GameService
import com.kiwixgames.dice.services.TableService
import com.kiwixgames.dice.services.WalletService
import com.kiwixgames.dice.services.rules.RulesEngine
import jakarta.persistence.EntityNotFoundException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TableServiceImpl(
    private val tableRepository: TableRepository,
    private val wagerLockRepository: WagerLockRepository,
    private val walletService: WalletService,
    private val gameService: GameService
) : TableService {

    private val rulesEngine = RulesEngine()

    @Transactional
    override fun createTable(owner: User, req: CreateTableRequest): GameTable {
        val rules = rulesEngine.resolve(req.mode, req.badgeTier, req.stakeGold)

        val table = tableRepository.save(
            GameTable(
                owner = owner,
                status = TableStatus.WAITING,
                mode = rules.mode,
                badgeTier = rules.badgeTier,
                stakeGold = rules.stakeGold,
                targetScore = rules.targetScore,
                seat0 = owner
            )
        )

        walletService.lockWager(owner, table.id!!, rules.stakeGold)
        wagerLockRepository.save(WagerLock(table = table, user = owner, stakeGold = rules.stakeGold))

        return table
    }

    override fun listWaiting(): List<GameTable> =
        tableRepository.findAllByStatus(TableStatus.WAITING)

    @Transactional
    override fun joinTable(user: User, tableId: Long): GameTable {
        val table = tableRepository.findById(tableId).orElseThrow {
            EntityNotFoundException("Table not found: $tableId")
        }

        if (table.status != TableStatus.WAITING) error("Table is not joinable")
        if (table.seat1 != null) error("Table already full")
        if (table.seat0?.id == user.id) error("Owner cannot join own table as opponent")

        try {
            // stake lock
            walletService.lockWager(user, table.id!!, table.stakeGold)
            wagerLockRepository.save(WagerLock(table = table, user = user, stakeGold = table.stakeGold))

            // update table
            table.seat1 = user
            table.status = TableStatus.IN_GAME
            val saved = tableRepository.save(table)

            // start game
            gameService.startGame(saved)

            return saved
        } catch (e: OptimisticLockingFailureException) {
            throw IllegalStateException("Table was joined by someone else. Please refresh and try again.")
        }
    }

    @Transactional
    override fun cancelTable(owner: User, tableId: Long): GameTable {
        val table = tableRepository.findById(tableId).orElseThrow {
            EntityNotFoundException("Table not found: $tableId")
        }
        if (table.owner.id != owner.id) error("Only owner can cancel")
        if (table.status != TableStatus.WAITING) error("Can only cancel WAITING tables")

        table.status = TableStatus.CANCELLED
        tableRepository.save(table)

        val lock = wagerLockRepository.findByTableIdAndUserId(tableId, owner.id!!)
            ?: error("Wager lock not found")
        if (lock.status == WagerLockStatus.LOCKED) {
            walletService.refundWager(owner, tableId, lock.stakeGold)
            lock.status = WagerLockStatus.REFUNDED
            wagerLockRepository.save(lock)
        }
        return table
    }
}
