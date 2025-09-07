import agents.MovieRecommendationAgent
import helpers.printMessageToUser
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        print("What do you want to watch? ")
        MovieRecommendationAgent()
            .agent
            .run(readln())
            .also { printMessageToUser(it.wrap()) }
    }
}

private fun String.wrap(): String = chunked(100).joinToString("\n") {
    if (it.last() != ' ') "${it}-" else it
}