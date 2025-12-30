package com.kiwixgames.dice.exceptions

import org.springframework.http.HttpStatus

class CustomAuthException(message: String, val status: HttpStatus) : RuntimeException(message)