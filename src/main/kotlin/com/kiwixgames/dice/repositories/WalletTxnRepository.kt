package com.kiwixgames.dice.repositories

import com.kiwixgames.dice.domain.entities.WalletTxn
import org.springframework.data.jpa.repository.JpaRepository

interface WalletTxnRepository : JpaRepository<WalletTxn, Long>
