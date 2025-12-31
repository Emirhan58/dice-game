package com.kiwixgames.dice.domain.model.game

data class RolledDie(
    val slot: Int,   // 0..5 (which dice)
    val value: Int   // 1..6
)