package com.kiwixgames.dice.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        // Frontend URL'lerini izin ver
        configuration.allowedOrigins = listOf(
            "http://localhost:3000",
            "http://localhost:3001",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:3001"
        )

        // Tüm HTTP metodlarına izin ver
        configuration.allowedMethods = listOf(
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "PATCH",
            "OPTIONS"
        )

        // Tüm header'lara izin ver
        configuration.allowedHeaders = listOf("*")

        // Credential'lara (cookies, authorization headers) izin ver
        configuration.allowCredentials = true

        // Expose edilecek header'lar
        configuration.exposedHeaders = listOf(
            "Authorization",
            "Set-Cookie"
        )

        // Preflight request cache süresi (1 saat)
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)

        return source
    }
}
