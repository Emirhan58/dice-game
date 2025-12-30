package com.kiwixgames.dice.repositories

import com.kiwixgames.dice.domain.entities.WagerLock
import org.springframework.data.jpa.repository.JpaRepository

interface WagerLockRepository : JpaRepository<WagerLock, Long> {
    fun findByTableIdAndUserId(tableId: Long, userId: Long): WagerLock?
    fun findAllByTableId(tableId: Long): List<WagerLock>
}