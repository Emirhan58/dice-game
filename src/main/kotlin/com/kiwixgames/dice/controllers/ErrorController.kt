package com.kiwixgames.dice.controllers

import com.kiwixgames.dice.domain.dtos.ApiErrorResponse
import com.kiwixgames.dice.exceptions.ConflictException
import com.kiwixgames.dice.exceptions.CustomAuthException
import com.kiwixgames.dice.exceptions.VirusDetectedException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.MalformedJwtException
import jakarta.persistence.EntityNotFoundException
import jakarta.servlet.http.HttpServletRequest
import org.apache.tomcat.util.http.fileupload.impl.FileCountLimitExceededException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authentication.AccountExpiredException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.CredentialsExpiredException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.authentication.LockedException
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestCookieException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.security.SignatureException
import kotlin.collections.map
import kotlin.jvm.java
import kotlin.text.orEmpty
import kotlin.to

@RestController
@ControllerAdvice
class ErrorController {

    private val logger = LoggerFactory.getLogger(ErrorController::class.java)

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ApiErrorResponse> {
        logger.error("Caught exception", ex)
        val error = ApiErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            message = "An unexpected error occurred"
        )
        return ResponseEntity(error, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            message = ex.message ?: "Invalid request"
        )
        return ResponseEntity(error, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            status = HttpStatus.CONFLICT.value(),
            message = ex.message ?: "Conflict occurred"
        )
        return ResponseEntity(error, HttpStatus.CONFLICT)
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(ex: BadCredentialsException): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            message = "Incorrect username or password"
        )
        return ResponseEntity(error, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(ex: EntityNotFoundException): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            message = ex.message ?: "Entity not found"
        )
        return ResponseEntity(error, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<ApiErrorResponse> {
        val fieldErrors = ex.bindingResult.fieldErrors.map {
            ApiErrorResponse.FieldError(
                field = it.field,
                message = it.defaultMessage ?: "Invalid value"
            )
        }

        val errorResponse = ApiErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            message = "Validation failed",
            errors = fieldErrors
        )

        return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(JwtException::class)
    fun handleJwtException(ex: JwtException): ResponseEntity<ApiErrorResponse> {
        val message = when (ex) {
            is ExpiredJwtException -> "Token has expired"
            is MalformedJwtException -> "Malformed token"
            else -> "Invalid JWT token"
        }

        val error = ApiErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            message = message,
            errors = null
        )
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error)
    }

    @ExceptionHandler(SignatureException::class)
    fun handleSignatureException(ex: SignatureException): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            message = "Invalid token signature",
            errors = null
        )
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error)
    }

    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleAuthorizationDeniedException(ex: AuthorizationDeniedException): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            status = HttpStatus.FORBIDDEN.value(),
            message = "Access denied",
            errors = null
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error)
    }

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingRequestHeaderException(ex: MissingRequestHeaderException): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            message = "UNAUTHORIZED",
            errors = null
        )
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error)
    }

    @ExceptionHandler(UsernameNotFoundException::class)
    fun handleUsernameNotFoundException(ex: UsernameNotFoundException): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            message = "No user found with given credentials"
        )
        return ResponseEntity(error, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(DisabledException::class)
    fun handleDisabledException(ex: DisabledException): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            message = "Account is disabled"
        )
        return ResponseEntity(error, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(
        AccountExpiredException::class,
        LockedException::class,
        CredentialsExpiredException::class
    )
    fun handleAccountExceptions(ex: RuntimeException): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            message = ex.message ?: "Authentication issue"
        )
        return ResponseEntity(error, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(MissingRequestCookieException::class)
    fun handleMissingRequestCookieException(ex: MissingRequestCookieException): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            message = "Missing Request Cookie",
            errors = null
        )
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error)
    }

    @ExceptionHandler(ConflictException::class)
    fun handleConflict(ex: ConflictException): ResponseEntity<Map<String, String>> {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(mapOf("error" to ex.message.orEmpty()))
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleHttpMediaTypeNotSupportedException(ex: HttpMediaTypeNotSupportedException): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            status = HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
            message = "UNSUPPORTED MEDIA TYPE",
            errors = null
        )
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(error)
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            status = HttpStatus.FORBIDDEN.value(),
            message = ex.message,
            errors = null
        )
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        ex: HttpMessageNotReadableException
    ): ResponseEntity<ApiErrorResponse> {

        val rootCauseMessage = ex.rootCause?.message ?: ex.message ?: "Malformed request body"

        val errorResponse = ApiErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            message = "Invalid request format: $rootCauseMessage",
            errors = null
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(CustomAuthException::class)
    fun handleCustomAuthException(
        ex: CustomAuthException,
        request: HttpServletRequest
    ): ResponseEntity<ApiErrorResponse> {
        val errorResponse = ApiErrorResponse(
            status = ex.status.value(),
            message = ex.message ?: "Authentication error",
            errors = null
        )
        return ResponseEntity.status(ex.status).body(errorResponse)
    }

    @ExceptionHandler(VirusDetectedException::class)
    fun handleVirusDetectedException(ex: VirusDetectedException): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            status = HttpStatus.UNPROCESSABLE_ENTITY.value(),
            message = ex.message ?: "Virus detected in uploaded file",
            errors = null
        )
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error)
    }

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(
        ex: NoResourceFoundException
    ): ResponseEntity<ApiErrorResponse> {
        val errorResponse = ApiErrorResponse(
            status = 404,
            message = ex.message ?: "No Resource Found",
            errors = null
        )
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(errorResponse)
    }

    @ExceptionHandler(FileCountLimitExceededException::class)
    fun handleFileCountLimitExceededException(ex: FileCountLimitExceededException): ResponseEntity<ApiErrorResponse> {
        val error = ApiErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            message = "File count limit exceeded"
        )
        return ResponseEntity(error, HttpStatus.BAD_REQUEST)
    }
}
