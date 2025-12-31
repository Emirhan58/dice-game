package com.kiwixgames.dice.controllers

import com.kiwixgames.dice.domain.dtos.wallet.WalletResponse
import com.kiwixgames.dice.domain.entities.User
import com.kiwixgames.dice.mappers.WalletMapper
import com.kiwixgames.dice.security.CurrentUserProvider
import com.kiwixgames.dice.services.WalletService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/wallet")
class WalletController(
    private val walletService: WalletService,
    private val currentUserProvider: CurrentUserProvider
) {
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    fun me(): ResponseEntity<WalletResponse> {
        val me: User = currentUserProvider.getUser()
        walletService.getOrCreateWallet(me)
        val balance: Long = walletService.getBalance(me.id!!)
        val dto: WalletResponse = WalletMapper.toDto(balance)
        return ResponseEntity.ok(dto)
    }
}
