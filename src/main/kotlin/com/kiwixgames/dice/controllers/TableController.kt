package com.kiwixgames.dice.controllers

import com.kiwixgames.dice.domain.dtos.table.CreateTableRequest
import com.kiwixgames.dice.domain.dtos.table.TableResponse
import com.kiwixgames.dice.domain.entities.GameTable
import com.kiwixgames.dice.domain.entities.User
import com.kiwixgames.dice.mappers.TableMapper
import com.kiwixgames.dice.repositories.GameRepository
import com.kiwixgames.dice.security.CurrentUserProvider
import com.kiwixgames.dice.services.TableService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/tables")
class TableController(
    private val tableService: TableService,
    private val currentUserProvider: CurrentUserProvider,
    private val gameRepository: GameRepository
) {

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    fun create(@Valid @RequestBody req: CreateTableRequest): ResponseEntity<TableResponse> {
        val me: User = currentUserProvider.getUser()
        val table: GameTable = tableService.createTable(me, req)
        val gameId: Long? = gameRepository.findByTableId(table.id!!)?.id
        val tableDto: TableResponse = TableMapper.toDto(table, gameId)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(tableDto)
    }

    @GetMapping("/waiting")
    fun waiting(): ResponseEntity<List<TableResponse>> {
        val listWaiting: List<GameTable> = tableService.listWaiting()
        val listWaitingDto: List<TableResponse> = listWaiting.map { TableMapper.toDto(it, gameRepository.findByTableId(it.id!!)?.id) }
        return ResponseEntity.ok(listWaitingDto)
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{tableId}/join")
    fun join(@PathVariable tableId: Long): ResponseEntity<TableResponse> {
        val me = currentUserProvider.getUser()
        val gameTable: GameTable = tableService.joinTable(me, tableId)
        val gameId: Long? = gameRepository.findByTableId(gameTable.id!!)?.id
        val gameTableDto: TableResponse = TableMapper.toDto(gameTable, gameId)
        return ResponseEntity.ok(gameTableDto)
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{tableId}/cancel")
    fun cancel(@PathVariable tableId: Long): ResponseEntity<TableResponse> {
        val me = currentUserProvider.getUser()
        val gameTable: GameTable =  tableService.cancelTable(me, tableId)
        val gameId: Long? = gameRepository.findByTableId(gameTable.id!!)?.id
        val gameTableDto: TableResponse = TableMapper.toDto(gameTable, gameId)
        return ResponseEntity.ok(gameTableDto)
    }
}