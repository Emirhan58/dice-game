package com.kiwixgames.dice.services.impl

import com.kiwixgames.dice.services.TokenBlacklistService
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class TokenBlacklistServiceImpl(
    private val redisTemplate: RedisTemplate<String, String>
) : TokenBlacklistService {

    override fun blacklistToken(token: String, expirationMillis: Long) {
        redisTemplate.opsForValue().set(token, "blacklisted", expirationMillis, TimeUnit.MILLISECONDS)
    }

    override fun isTokenBlacklisted(token: String): Boolean {
        return redisTemplate.hasKey(token)
    }
}