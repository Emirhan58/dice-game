package com.kiwixgames.dice.services.impl

import com.kiwixgames.dice.domain.entities.User
import com.kiwixgames.dice.domain.entities.Wallet
import com.kiwixgames.dice.domain.entities.WalletTxn
import com.kiwixgames.dice.domain.enums.WalletTxnType
import com.kiwixgames.dice.repositories.WalletRepository
import com.kiwixgames.dice.repositories.WalletTxnRepository
import com.kiwixgames.dice.services.WalletService
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WalletServiceImpl(
    private val walletRepository: WalletRepository,
    private val walletTxnRepository: WalletTxnRepository
) : WalletService {

    @Transactional
    override fun getOrCreateWallet(user: User): Long {
        val existing = walletRepository.findByUserId(user.id!!)
        if (existing != null) return existing.balanceGold
        val w = walletRepository.save(Wallet(user = user, balanceGold = 0))
        return w.balanceGold
    }

    override fun getBalance(userId: Long): Long {
        return walletRepository.findByUserId(userId)?.balanceGold
            ?: 0
    }

    @Transactional
    override fun lockWager(user: User, tableId: Long, stakeGold: Int) {
        val wallet = walletRepository.findByUserId(user.id!!)
            ?: walletRepository.save(Wallet(user = user, balanceGold = 0))

        val stake = stakeGold.toLong()
        if (wallet.balanceGold < stake) {
            throw IllegalStateException("Insufficient gold. balance=${wallet.balanceGold}, required=$stake")
        }

        wallet.balanceGold -= stake
        walletRepository.save(wallet)

        walletTxnRepository.save(
            WalletTxn(
                user = user,
                amount = -stake,
                type = WalletTxnType.WAGER_LOCK,
                refType = "TABLE",
                refId = tableId,
                note = "Wager locked for table $tableId"
            )
        )
    }

    @Transactional
    override fun refundWager(user: User, tableId: Long, stakeGold: Int) {
        val wallet = walletRepository.findByUserId(user.id!!)
            ?: throw EntityNotFoundException("Wallet not found for userId=${user.id}")

        val stake = stakeGold.toLong()
        wallet.balanceGold += stake
        walletRepository.save(wallet)

        walletTxnRepository.save(
            WalletTxn(
                user = user,
                amount = stake,
                type = WalletTxnType.WAGER_REFUND,
                refType = "TABLE",
                refId = tableId,
                note = "Wager refunded for table $tableId"
            )
        )
    }

    @Transactional
    override fun payoutWinner(winner: User, tableId: Long, amountGold: Int) {
        val wallet = walletRepository.findByUserId(winner.id!!)
            ?: walletRepository.save(Wallet(user = winner, balanceGold = 0))

        val amount = amountGold.toLong()
        wallet.balanceGold += amount
        walletRepository.save(wallet)

        walletTxnRepository.save(
            WalletTxn(
                user = winner,
                amount = amount,
                type = WalletTxnType.PAYOUT,
                refType = "TABLE",
                refId = tableId,
                note = "Payout for table $tableId"
            )
        )
    }
}
