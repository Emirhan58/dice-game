package com.kiwixgames.dice.mappers

import com.kiwixgames.dice.domain.dtos.wallet.WalletResponse

object WalletMapper {
    fun toDto(balanceGold: Long): WalletResponse = WalletResponse(balanceGold = balanceGold)
}