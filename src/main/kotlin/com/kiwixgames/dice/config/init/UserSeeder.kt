package com.kiwixgames.dice.config.init

import com.kiwixgames.dice.domain.entities.User
import com.kiwixgames.dice.domain.enums.Role
import com.kiwixgames.dice.repositories.UserRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class UserSeeder(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    override fun run(vararg args: String) {
        if (userRepository.count() == 0L) {
            val user = User(
                username = "admin",
                email = "admin@dev.com",
                password = passwordEncoder.encode("1234")!!,
                role = Role.ADMIN
            )
            userRepository.save(user)
            println("📦 Seed admin user created!")
        } else {
            println("ℹ️ Seed Admin user already created.")
        }
    }
}
