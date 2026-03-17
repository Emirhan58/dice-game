package com.kiwixgames.dice.controllers

import com.kiwixgames.dice.domain.dtos.game.GameStateResponse
import com.kiwixgames.dice.domain.dtos.game.KeepRequest
import com.kiwixgames.dice.domain.entities.Game
import com.kiwixgames.dice.domain.entities.User
import com.kiwixgames.dice.domain.model.game.GameState
import com.kiwixgames.dice.mappers.GameMapper
import com.kiwixgames.dice.repositories.GameRepository
import com.kiwixgames.dice.security.CurrentUserProvider
import com.kiwixgames.dice.services.GamePlayService
import com.kiwixgames.dice.services.PlayerPresenceService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/games")
class GameController(
    private val gameRepository: GameRepository,
    private val gamePlayService: GamePlayService,
    private val currentUserProvider: CurrentUserProvider,
    private val playerPresenceService: PlayerPresenceService
) {

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{gameId}")
    fun get(@PathVariable gameId: Long): ResponseEntity<GameStateResponse> {
        val me: User = currentUserProvider.getUser()
        val game: Game = gameRepository.findById(gameId).orElseThrow { IllegalArgumentException("Game not found") }
        val state: GameState = gamePlayService.getState(gameId, me)
        val mySeat: Int = resolveSeat(game, me)
        val response: GameStateResponse = GameMapper.toDto(game, state, mySeat)
        return ResponseEntity.ok(response)
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{gameId}/roll")
    fun roll(@PathVariable gameId: Long): ResponseEntity<GameStateResponse> {
        val me: User = currentUserProvider.getUser()
        val game: Game = gameRepository.findById(gameId).orElseThrow { IllegalArgumentException("Game not found") }
        val state: GameState = gamePlayService.roll(gameId, me)
        val mySeat: Int = resolveSeat(game, me)
        val response: GameStateResponse = GameMapper.toDto(game, state, mySeat)
        return ResponseEntity.ok(response)
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{gameId}/keep")
    fun keep(
        @PathVariable gameId: Long,
        @Valid @RequestBody req: KeepRequest
    ): ResponseEntity<GameStateResponse> {
        val me: User = currentUserProvider.getUser()
        val game: Game = gameRepository.findById(gameId).orElseThrow { IllegalArgumentException("Game not found") }
        val state: GameState = gamePlayService.keep(gameId, me, req.slots)
        val mySeat: Int = resolveSeat(game, me)
        val response: GameStateResponse = GameMapper.toDto(game, state, mySeat)
        return ResponseEntity.ok(response)
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{gameId}/bank")
    fun bank(@PathVariable gameId: Long): ResponseEntity<GameStateResponse> {
        val me: User = currentUserProvider.getUser()
        val game: Game = gameRepository.findById(gameId).orElseThrow { IllegalArgumentException("Game not found") }
        val state: GameState = gamePlayService.bank(gameId, me)
        val mySeat: Int = resolveSeat(game, me)
        val response : GameStateResponse = GameMapper.toDto(game, state, mySeat)
        return ResponseEntity.ok(response)
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{gameId}/forfeit")
    fun forfeit(@PathVariable gameId: Long): ResponseEntity<GameStateResponse> {
        val me: User = currentUserProvider.getUser()
        val game: Game = gameRepository.findById(gameId).orElseThrow { IllegalArgumentException("Game not found") }
        val state: GameState = gamePlayService.forfeit(gameId, me)
        val mySeat: Int = resolveSeat(game, me)
        val response: GameStateResponse = GameMapper.toDto(game, state, mySeat)
        return ResponseEntity.ok(response)
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/{gameId}/ping")
    fun ping(@PathVariable gameId: Long): ResponseEntity<Void> {
        val me: User = currentUserProvider.getUser()
        val game: Game = gameRepository.findById(gameId).orElseThrow { IllegalArgumentException("Game not found") }
        resolveSeat(game, me) // throws if not a player
        playerPresenceService.ping(gameId, me.id!!)
        return ResponseEntity.ok().build()
    }

    private fun resolveSeat(game: Game, me: User): Int {
        val myId = me.id ?: error("user id null")
        return when (myId) {
            game.table.seat0?.id -> 0
            game.table.seat1?.id -> 1
            else -> error("Not a player")
        }
    }
}
