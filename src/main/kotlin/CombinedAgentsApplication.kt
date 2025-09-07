import agents.MovieRecommendationAgent
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.AIAgentTool
import ai.koog.agents.core.agent.asTool
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteSingleTool
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.environment.SafeTool
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.OllamaModels
import helpers.print
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

fun main() {
    runBlocking {
        val movieRecommendationTool = MovieRecommendationAgent(verbose = false).agent.asTool(
            agentName = "movieRecommendation",
            agentDescription = "Recommends a movie based on a user query",
            inputDescriptor = ToolParameterDescriptor(
                name = "request",
                description = "User query",
                type = ToolParameterType.String,
            )
        )

        val fileSystemMCPProcess = ProcessBuilder(
            "docker",
            "run",
            "-i",
            "--rm",
            "-v",
            "./ai-output:/ai-output",
            "mcp/filesystem",
            "/ai-output"
        ).start()

        val toolRegistry = McpToolRegistryProvider.fromTransport(
            transport = McpToolRegistryProvider.defaultStdioTransport(fileSystemMCPProcess)
        )

        val strategy = strategy<String, String>("Movie recommendation saved to file") {

            val nodeMovieRecommendation by nodeExecuteSingleTool(tool = movieRecommendationTool)
            val tellFileCreated by node<ReceivedToolResult, String> {
                llm.writeSession {
                    updatePrompt {
                        assistant("File created")
                    }

                    "Done"
                }
            }
            val nodeUpdatePrompt by node<SafeTool.Result<AIAgentTool.AgentToolResult>, SafeTool.Result<AIAgentTool.AgentToolResult>> {
                llm.writeSession {
                    updatePrompt {
                        user("Now create the file /ai-output/movie-recommendation.txt with content: ${it.asSuccessful().result.result}")
                    }
                }
                it
            }
            val nodeLlmRequest by nodeLLMRequest()
            val nodeExecuteTool by nodeExecuteTool()

            edge(nodeStart forwardTo nodeMovieRecommendation transformed {
                AIAgentTool.AgentToolArgs(JsonObject(mapOf("request" to JsonPrimitive(it))))
            })

            edge(nodeMovieRecommendation forwardTo nodeUpdatePrompt)

            edge(nodeUpdatePrompt forwardTo nodeLlmRequest transformed { it.content })

            edge(nodeLlmRequest forwardTo nodeExecuteTool onToolCall { true })

            edge(nodeExecuteTool forwardTo tellFileCreated)

            edge(tellFileCreated forwardTo nodeFinish)
        }

        AIAgent(
            executor = simpleOllamaAIExecutor(),
            llmModel = OllamaModels.Meta.LLAMA_3_2_3B,
            toolRegistry = toolRegistry.apply { add(movieRecommendationTool) },
            installFeatures = {
                install(EventHandler) {
                    onToolCall { println("Tool ${it.tool.name} called with args ${it.toolArgs}") }
                    onAfterLLMCall {
                        println("LLM called. Prompt messages so far:")
                        it.prompt.print()
                    }
                }
            },
            strategy = strategy,
        )
            .run("I want to watch a movie with a lot of shooting")
            .also { println(it) }
    }
}