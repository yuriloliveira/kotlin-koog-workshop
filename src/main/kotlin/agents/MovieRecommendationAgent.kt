package agents

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.entity.AIAgentStrategy
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
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.OllamaModels
import ai.koog.prompt.message.Message
import helpers.CsvReader
import helpers.print
import helpers.printMessageToUser
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import java.io.FileNotFoundException

class MovieRecommendationAgent(verbose: Boolean = true) {
    val agent: AIAgent<String, String> = AIAgent(
        executor = simpleOllamaAIExecutor(),
        llmModel = OllamaModels.Meta.LLAMA_3_2_3B,
        strategy = buildStrategy(),
        toolRegistry = ToolRegistry { tool(MovieByGenreTool) },
        installFeatures = {
            if (verbose) {
                install(EventHandler) {
                    onBeforeAgentStarted { eventContext: AgentStartContext<*> ->
                        println("Starting strategy: ${eventContext.strategy.name}")
                    }
                    onAgentFinished { _: AgentFinishedContext ->
                        println("Agent has finished!")
                    }
                    onAfterLLMCall { ctx ->
                        println("LLM was called. Prompt messages so far:")
                        val prompt = ctx.prompt
                        prompt.print()
                    }
                    onToolCall { eventContext: ToolCallContext ->
                        println("Tool '${eventContext.tool.name}' called with context: ${eventContext.toolArgs}")
                    }
                }
            }
        }
    )

    private fun buildStrategy(): AIAgentStrategy<String, String> = strategy<String, String>("Movie recommendation") {
        val toMovieCategoryNode by node<String, String> { input ->
            llm.writeSession {
                updatePrompt {
                    system(
                        """You're a helpful assistant that will help understanding what the person wishes to watch.
                                |Identify the movie genre within one of: ${
                            moviesByGenre.keys.joinToString(
                                "\n",
                                prefix = "\n"
                            ) { "- $it" }
                        }
                                |Provide the movie genre only.""".trimMargin()
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
                        "You're an movie expert that gives movie descriptions based on their names. The description are composed of a single sentence of up to 20 words."
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

    object MovieByGenreTool: SimpleTool<MovieByGenreTool.MovieGenre>() {
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
            val movies = moviesByGenre[args.genre.lowercase()]
            return movies?.randomOrNull() ?: "Couldn't find a movie with given genre..."
        }

        @Serializable
        data class MovieGenre(val genre: String): ToolArgs
    }
}


private const val GENRE_HEADER = "genre"
private const val MOVIE_TITLE_HEADER = "movie title"

val moviesByGenre: Map<String, List<String>> by lazy {
    val inputStream =
        MovieRecommendationAgent.MovieByGenreTool::class.java.classLoader.getResourceAsStream("movies.csv")
            ?: throw FileNotFoundException("File movies.csv could not be found")

    CsvReader.readCSV(
        inputStream,
        headers = listOf(MOVIE_TITLE_HEADER, GENRE_HEADER, "rating", "description"),
    )
        .groupBy(keySelector = {
            it[GENRE_HEADER]?.lowercase() ?: throw RuntimeException("Genre could not be found")
        }) {
            it[MOVIE_TITLE_HEADER] ?: throw RuntimeException("Movie title could not be found")
        }
}