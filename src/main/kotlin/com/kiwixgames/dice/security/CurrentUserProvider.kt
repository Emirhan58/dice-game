package com.kiwixgames.dice.security

import com.kiwixgames.dice.repositories.UserRepository
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Component
class CurrentUserProvider(
    private val userRepository: UserRepository
) {
    fun getUser() = run {
        val usernameOrEmail = when (val principal = SecurityContextHolder.getContext().authentication?.principal) {
            is org.springframework.security.core.userdetails.UserDetails -> principal.username
            is String -> principal
            else -> error("Unknown principal type")
        }
        userRepository.findByEmail(usernameOrEmail) ?: userRepository.findByUsername(usernameOrEmail)
        ?: error("User not found for principal")
    }
}
