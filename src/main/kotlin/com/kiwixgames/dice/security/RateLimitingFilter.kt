package com.kiwixgames.dice.security

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Bucket4j
import io.github.bucket4j.Refill
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitingFilter : OncePerRequestFilter() {

    private val buckets = ConcurrentHashMap<String, Bucket>()

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val key = resolveRateLimitKey(request)
        val bucket = buckets.computeIfAbsent(key) { createNewBucket(key) }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response)
        } else {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.contentType = "application/json"
            response.characterEncoding = "UTF-8"
            val writer = response.writer
            writer.write(
                """{
                    "status": 429,
                    "message": "Too many requests. Please slow down."
                }""".trimIndent()
            )
            writer.flush()
        }
    }

    private fun resolveRateLimitKey(request: HttpServletRequest): String {
        val userId = request.getAttribute("userId")?.toString()
        return userId ?: request.remoteAddr
    }

    private fun createNewBucket(key: String): Bucket {
        val limit = if (key.matches(Regex("\\d+"))) {
            Bandwidth.classic(100, Refill.intervally(10, Duration.ofSeconds(1)))
        } else {
            Bandwidth.classic(10, Refill.intervally(1, Duration.ofSeconds(6)))
        }
        return Bucket4j.builder()
            .addLimit(limit)
            .build()
    }
}
