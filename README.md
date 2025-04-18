# KastraX

KastraX is a modern AI Agent framework built in Kotlin, providing a comprehensive set of tools and abstractions for building AI-powered applications with a focus on type safety, modularity, and developer experience.

## Features

- **Agent System**: Create AI agents with a fluent DSL
- **LLM Abstraction**: Unified interface for different LLM providers
- **Tool System**: Allow agents to interact with external systems
- **Type Safety**: Strong typing throughout the framework
- **Kotlin-first**: Leverage Kotlin's language features for a seamless developer experience

## Getting Started

### Prerequisites

- JDK 17 or higher
- Gradle 8.0 or higher

### Installation

Add the following to your `build.gradle.kts` file:

```kotlin
dependencies {
    implementation("ai.kastrax:kastrax-core:0.1.0")
    implementation("ai.kastrax:kastrax-integrations:kastrax-openai:0.1.0") // Optional
}
```

### Basic Usage

```kotlin
import ai.kastrax.core.agent.agent
import ai.kastrax.core.tools.tool
import ai.kastrax.integrations.openai.openAi

// Create an agent
val myAgent = agent {
    name = "Assistant"
    instructions = "You are a helpful assistant."
    model = openAi(
        model = "gpt-4o",
        // API key from environment variable OPENAI_API_KEY
    )
    
    // Add tools
    tools {
        tool {
            id = "calculator"
            name = "Calculator"
            description = "Perform mathematical calculations"
            // Define input/output schemas and execution logic
            // ...
        }
    }
}

// Use the agent
val response = myAgent.generate("What is the capital of France?")
println(response.text)

// Stream responses
myAgent.stream("Tell me a story") { chunk ->
    print(chunk)
}
```

## Project Structure

KastraX follows a modular architecture with the following components:

- **kastrax-core**: Core framework components
- **kastrax-memory**: Memory and storage systems
- **kastrax-integrations**: Third-party integrations
  - **kastrax-openai**: OpenAI integration
  - **kastrax-anthropic**: Anthropic integration (coming soon)
  - **kastrax-gemini**: Google Gemini integration (coming soon)
  - **kastrax-mistral**: Mistral integration (coming soon)
- **kastrax-rag**: Retrieval-augmented generation (coming soon)
- **kastrax-cli**: Command-line tools (coming soon)
- **kastrax-evals**: Evaluation framework (coming soon)

## License

This project is licensed under the MIT License - see the LICENSE file for details.
