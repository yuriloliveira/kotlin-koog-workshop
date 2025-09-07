import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.OllamaModels
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        val fileSystemMCPProcess = ProcessBuilder(
            "docker",
            "run",
            "-i",
            "--rm",
            "-v",
            "./ai-assistant:/ai",
            "mcp/filesystem",
            "/ai"
        ).start()

        val toolRegistry = McpToolRegistryProvider.fromTransport(
            transport = McpToolRegistryProvider.defaultStdioTransport(fileSystemMCPProcess)
        )

        val agent = AIAgent(
            executor = simpleOllamaAIExecutor(),
            llmModel = OllamaModels.Meta.LLAMA_3_2_3B,
            toolRegistry = toolRegistry,
        )

        agent.run("""Create a file movie-recommendation.txt in folder '/ai'
            | where the content is a random funny sentence.""".trimMargin()
        )
    }
}