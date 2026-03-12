package com.kiwixgames.dice.services

import com.kiwixgames.dice.domain.entities.User

interface WalletService {
    fun getOrCreateWallet(user: User): Long
    fun getBalance(userId: Long): Long
    fun lockWager(user: User, tableId: Long, stakeGold: Int)
    fun refundWager(user: User, tableId: Long, stakeGold: Int)
    fun payoutWinner(winner: User, tableId: Long, amountGold: Int)
    fun payoutOnce(tableId: Long, winnerSeat: Int)
}
