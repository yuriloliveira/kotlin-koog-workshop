import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.OllamaModels
import helpers.printMessageToUser
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        val executor: SingleLLMPromptExecutor =
            simpleOllamaAIExecutor()
//            simpleAnthropicExecutor(System.getenv("CLAUDE_APIKEY"))
        val llmModel =
            OllamaModels.Meta.LLAMA_3_2_3B
//            AnthropicModels.Sonnet_4,
        val agent = AIAgent(
            executor = executor,
            systemPrompt = """
                You are a experienced software developer that is very enthusiastic about Kotlin.
                You're assisting in a workshop to a audience that's also very enthusiastic about Kotlin.
            """.trimIndent(),
            llmModel = llmModel
        )

        val result = agent.run("""Hello!
            |Can you explain me in short text, using bullet points,
            |what is the big difference between Kotlin Koog and Spring AI?"""
            .trimMargin()
        )
        printMessageToUser(result)
    }
}