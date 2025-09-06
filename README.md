# Kotlin Koog workshop
This workshop contains many examples. They go through how to run agents with Kotlin Koog.

## Pre-requisites
- Docker
- Ollama
- Gradle
- Kotlin

## Setting up LLM models with Ollama
If you haven't yet, install [Ollama](https://ollama.com).

Run ollama locally with docker:
```shell
docker run -d -v ollama:/root/.ollama -p 11434:11434 --name ollama ollama/ollama
```

Once ollama is running, it's necessary to pull the model(s). In this workshop we're using `llama3.2:3b`. To pull it, run:
```shell
ollama pull llama3.2:3b
```

If you're faced with error
<pre style="color: #d00;">
Exception in thread "main" java.lang.RuntimeException: Ollama API error: model "(model-name)" not found, try pulling it first
</pre>

when trying to run the application, you're missing the model in ollama. Just run the `ollama pull (model-name)` with the proper model name.