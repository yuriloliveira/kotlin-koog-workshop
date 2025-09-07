package agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.OllamaModels
import kotlinx.coroutines.runBlocking

class CreateFileAgent {
    val agent: AIAgent<String, String> = runBlocking {
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

        AIAgent(
            executor = simpleOllamaAIExecutor(),
            llmModel = OllamaModels.Meta.LLAMA_3_2_3B,
            toolRegistry = toolRegistry,
            systemPrompt = """You are a file manager that creates files. Prepend the filenames with '/ai/'.
                | You create the folder in case it doesn't exist""".trimMargin(),
            installFeatures = {
                install(EventHandler) {
                    onToolCall {
                        println("Tool called ${it.tool.name} with args ${it.toolArgs}")
                    }
                }
            }
        )
    }
}