package com.kiwixgames.dice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DiceApplication

fun main(args: Array<String>) {
	runApplication<DiceApplication>(*args)
}
