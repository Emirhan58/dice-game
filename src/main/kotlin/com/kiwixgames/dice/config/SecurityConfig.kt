package com.kiwixgames.dice.config

import com.kiwixgames.dice.repositories.UserRepository
import com.kiwixgames.dice.security.BlogUserDetailsService
import com.kiwixgames.dice.security.JwtAuthenticationFilter
import com.kiwixgames.dice.security.RateLimitingFilter
import com.kiwixgames.dice.services.AuthenticationService
import com.kiwixgames.dice.services.TokenBlacklistService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
class SecurityConfig {

    @Bean
    fun jwtAuthenticationFilter(
        authenticationService: AuthenticationService,
        tokenBlacklistService: TokenBlacklistService
    ): JwtAuthenticationFilter {
        return JwtAuthenticationFilter(authenticationService, tokenBlacklistService)
    }

    @Bean
    fun userDetailsService(
        userRepository: UserRepository
    ): UserDetailsService {
        return BlogUserDetailsService(userRepository)
    }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        jwtAuthenticationFilter: JwtAuthenticationFilter,
        rateLimitingFilter: RateLimitingFilter,
        corsConfigurationSource: CorsConfigurationSource
    ): SecurityFilterChain {
        http
            .cors { it.configurationSource(corsConfigurationSource) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.anyRequest().permitAll()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(rateLimitingFilter, JwtAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder()
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager {
        return config.authenticationManager
    }
}
