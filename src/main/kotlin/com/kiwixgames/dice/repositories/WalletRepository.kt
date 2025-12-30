package com.kiwixgames.dice.repositories

import com.kiwixgames.dice.domain.entities.Wallet
import org.springframework.data.jpa.repository.JpaRepository

interface WalletRepository : JpaRepository<Wallet, Long> {
    fun findByUserId(userId: Long): Wallet?
}