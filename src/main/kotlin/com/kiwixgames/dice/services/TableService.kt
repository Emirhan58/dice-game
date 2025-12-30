package com.kiwixgames.dice.services

import com.kiwixgames.dice.domain.dtos.table.CreateTableRequest
import com.kiwixgames.dice.domain.entities.GameTable
import com.kiwixgames.dice.domain.entities.User

interface TableService {
    fun createTable(owner: User, req: CreateTableRequest): GameTable
    fun listWaiting(): List<GameTable>
    fun joinTable(user: User, tableId: Long): GameTable
    fun cancelTable(owner: User, tableId: Long): GameTable
}
