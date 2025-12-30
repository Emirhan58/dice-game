package com.kiwixgames.dice.domain.dtos

data class ApiErrorResponse(
    val status: Int,
    val message: String,
    val errors: List<FieldError>? = null
) {
    data class FieldError(
        val field: String,
        val message: String
    )
}