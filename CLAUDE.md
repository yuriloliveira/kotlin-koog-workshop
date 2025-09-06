# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin workshop project demonstrating the Koog AI agent framework. The project consists of a simple Kotlin application that creates an AI agent using the Koog library and 
demonstrates interactions with LLM models.

## Key Components
        
- **KoogApplication.kt**: Main application entry point that demonstrates creating and using AI agents with the Koog framework
 - **Dependencies**: Uses Koog agents library (0.4.1), Kotlin coroutines, and SLF4J for logging
  - **LLM Integration**: Configured to work with Ollama (Llama 3.2 3B model) and Anthropic Claude models
   
   ## Build System

This project uses Gradle with Kotlin DSL:

- **Build**: `./gradlew build` 
 - **Run**: `./gradlew run`
  - **Test**: `./gradlew test`
   - **Clean**: `./gradlew clean`
    
    Note: The Gradle wrapper may have compatibility issues with certain hardware platforms. If you encounter build errors, try using a different JVM or Gradle version.

## Project Structure

```
kotlin-koog-workshop/
├── src/main/kotlin/
│   └── KoogApplication.kt    # Main application demonstrating Koog AI agents
├── build.gradle.kts          # Gradle build configuration
├── settings.gradle.kts       # Gradle settings
└── gradle.properties         # Gradle properties
```
       
## Development Notes

- Uses Kotlin 2.2.0 with JVM toolchain 24
- No test files currently present in the project
- The application demonstrates both Ollama and Anthropic model configurations
- System prompts are configured for Kotlin workshop context

## AI Agent Configuration

The main application shows how to:
1. Create SingleLLMPromptExecutor instances
2. Configure AI agents with system prompts
3. Run queries against the configured LLM models
4. Handle responses from AI agents
        
The project is specifically designed as a workshop example comparing Koog and Spring AI frameworks.