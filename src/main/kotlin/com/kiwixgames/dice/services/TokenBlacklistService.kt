package com.kiwixgames.dice.services

interface TokenBlacklistService {
    fun blacklistToken(token: String, expirationMillis: Long)
    fun isTokenBlacklisted(token: String): Boolean
}