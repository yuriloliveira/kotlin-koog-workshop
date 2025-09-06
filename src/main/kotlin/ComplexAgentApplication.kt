import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.agents.core.feature.handler.AgentFinishedContext
import ai.koog.agents.core.feature.handler.AgentStartContext
import ai.koog.agents.core.feature.handler.ToolCallContext
import ai.koog.agents.core.tools.*
import ai.koog.agents.ext.tool.SayToUser
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.OllamaModels
import ai.koog.prompt.message.Message
import helpers.printMessageToUser
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

fun main() {
    runBlocking {
        // Create an strategy that will update the initial prompt
        // just get the genre of the movie the person wants to watch

        val executor = simpleOllamaAIExecutor()
        val llmModel = OllamaModels.Meta.LLAMA_3_2_3B

        val agentStrategy = strategy<String, String>("Movie recommendation") {
            val toMovieCategoryNode by node<String, String> { input ->
                llm.writeSession {
                    updatePrompt {
                        system(
                            """You're a helpful assistant that will help understanding what the person wishes to watch.
                            |Identify the movie genre and provide the genre. Provide the movie genre only.
                            |""".trimMargin()
                        )
                    }
                }

                input
            }

            val processMovieGenreNode by node<String, String> { input ->
                input.also {
                    printMessageToUser("I recommend you watch $input!")
                }
            }

            val askLLMtoExplainMovieDetails by node<String, Message.Response> { input ->
                llm.writeSession {
                    updatePrompt {
                        system(
                            "You're an movie expert that gives concise movie descriptions based on their names."
                        )
                        user("Can you describe the movie '$input' to me?")

                    }
                    requestLLMWithoutTools()
                }
            }

            val nodeSendInput by nodeLLMRequest()
            val executeMovieTool by nodeExecuteTool("MovieTool")

            edge(nodeStart forwardTo toMovieCategoryNode)
            edge(toMovieCategoryNode forwardTo nodeSendInput)
            edge(nodeSendInput forwardTo executeMovieTool onToolCall { true })
            edge(executeMovieTool forwardTo processMovieGenreNode transformed { it.content })
            edge(processMovieGenreNode forwardTo askLLMtoExplainMovieDetails)
            edge(askLLMtoExplainMovieDetails forwardTo nodeFinish onAssistantMessage { true })
        }

        val agent = AIAgent(
            executor = executor,
            llmModel = llmModel,
            strategy = agentStrategy,
            toolRegistry = ToolRegistry {
                tool(MovieByGenreTool())
                tool(SayToUser)
            },
            installFeatures = {
                install(EventHandler) {
                    onBeforeAgentStarted { eventContext: AgentStartContext<*> ->
                        println("Starting strategy: ${eventContext.strategy.name}")
                    }
                    onAgentFinished { _: AgentFinishedContext ->
                        println("Agent has finished!")
                    }
                    onAfterLLMCall { ctx ->
                        println("LLM was called. Prompt messages so far:")
                        ctx.prompt.messages.forEach {
                            println("> [${it.metaInfo.timestamp}] [${it.role}] ${it.content}")
                        }
                    }
                    onToolCall { eventContext: ToolCallContext ->
                        println("Tool '${eventContext.tool.name}' called with context: ${eventContext.toolArgs}")
                    }
                }
            }
        )

        agent
            .run("I want to watch a movie to laugh a lot!")
            .also { printMessageToUser(it.wrap()) }
    }
}

private fun String.wrap(): String = chunked(100).joinToString("\n") {
    if (it.last() != ' ') "${it}-" else it
}

class MovieByGenreTool: SimpleTool<MovieByGenreTool.MovieGenre>() {
    override val argsSerializer: KSerializer<MovieGenre>
        get() = MovieGenre.serializer()

    override val descriptor: ToolDescriptor
        get() = ToolDescriptor(
            name = "recommend_movie_by_genre",
            description = "Recommends a movie by the genre. The resulting movie doesn't have to verified.",
            requiredParameters = listOf(
                ToolParameterDescriptor(
                    name = "genre",
                    description = "The movie genre",
                    type = ToolParameterType.String,
                )
            )
        )

    override suspend fun doExecute(args: MovieGenre): String {
        printMessageToUser("Looks like you want to watch a ${args.genre} movie.")
        return "The Simpsons"
    }

    @Serializable
    data class MovieGenre(val genre: String): ToolArgs
}