package helpers

import helpers.ConsoleColors.BLUE
import helpers.ConsoleColors.RESET

fun printMessageToUser(message: String) {
    println("${BLUE}$message${RESET}")
}