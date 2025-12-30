package com.kiwixgames.dice.repositories

import com.kiwixgames.dice.domain.entities.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long>{
    fun findByUsername(username: String): User?
    fun findByEmail(email: String): User?
    fun existsByEmail(name: String): Boolean
    fun existsByUsername(name: String): Boolean
    fun findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
        username: String, email: String, pageable: Pageable
    ): Page<User>
}