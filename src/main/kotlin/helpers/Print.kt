package helpers

import ai.koog.prompt.dsl.Prompt
import helpers.ConsoleColors.BLUE
import helpers.ConsoleColors.RESET

fun printMessageToUser(message: String) {
    println("${BLUE}$message${RESET}")
}

fun Prompt.print() {
    messages.forEach {
        println("> [${it.metaInfo.timestamp}] [${it.role}] ${it.content}")
    }
}