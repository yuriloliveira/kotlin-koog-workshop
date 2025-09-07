import agents.CreateFileAgent
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        CreateFileAgent()
            .agent
            .run("Create a file movie-recommendation.txt with content 'dummy content'.")
    }
}