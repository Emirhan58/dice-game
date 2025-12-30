package com.kiwixgames.dice.security

import com.kiwixgames.dice.domain.entities.User
import com.kiwixgames.dice.repositories.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException

class BlogUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    override fun loadUserByUsername(email: String): UserDetails {
        val user: User = userRepository.findByEmail(email)
            ?: throw UsernameNotFoundException("User not found with email: $email")

        return BlogUserDetails(user)
    }
}