package com.kiwixgames.dice.mappers

import com.kiwixgames.dice.domain.dtos.table.TableResponse
import com.kiwixgames.dice.domain.entities.GameTable

object TableMapper {
    fun toDto(gameTable: GameTable): TableResponse {
        return TableResponse(
            id = gameTable.id!!,
            status = gameTable.status.name,
            mode = gameTable.mode.name,
            badgeTier = gameTable.badgeTier?.name,
            stakeGold = gameTable.stakeGold,
            targetScore = gameTable.targetScore,
            seat0UserId = gameTable.seat0?.id,
            seat1UserId = gameTable.seat1?.id
        )
    }
}