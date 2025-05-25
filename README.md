# KastraX

<div align="center">
  <img src="docs/assets/kastrax-logo.png" alt="KastraX Logo" width="200" height="auto" />
  <br>
  <p><strong>Modern AI Agent Framework Built in Kotlin</strong></p>
  
  [![Build Status](https://img.shields.io/github/workflow/status/kastrax-ai/kastrax/CI)](https://github.com/kastrax-ai/kastrax/actions)
  [![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
  [![Kotlin](https://img.shields.io/badge/kotlin-2.1.10-blue.svg)](https://kotlinlang.org)
  [![Maven Central](https://img.shields.io/maven-central/v/ai.kastrax/kastrax-core.svg)](https://search.maven.org/search?q=g:ai.kastrax)
  [![Discord](https://img.shields.io/discord/1234567890?color=5865F2&label=discord)](https://discord.gg/kastrax)
</div>

KastraX is a modern AI Agent framework built in Kotlin, providing a comprehensive set of tools and abstractions for building AI-powered applications with a focus on type safety, modularity, and developer experience.

## Table of Contents

- [Features](#features)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Installation](#installation)
  - [Basic Usage](#basic-usage)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Documentation](#documentation)
- [Examples](#examples)
- [Advanced Use Cases](#advanced-use-cases)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [Community](#community)
- [License](#license)
- [Inspiration & Acknowledgments](#inspiration--acknowledgments)

## Features

- **Agent System**: Create AI agents with a fluent DSL
- **LLM Abstraction**: Unified interface for different LLM providers
- **Tool System**: Allow agents to interact with external systems
- **Type Safety**: Strong typing throughout the framework
- **Kotlin-first**: Leverage Kotlin's language features for a seamless developer experience
- **RAG Support**: Retrieval-augmented generation capabilities
- **Memory Systems**: Flexible memory management for agents
- **Agent-to-Agent Communication**: Build multi-agent systems with structured communication
- **Vector Storage**: Multiple vector store integrations for embedding storage
- **Observability**: Built-in monitoring and logging tools

## Getting Started

### Prerequisites

- JDK 17 or higher
- Gradle 8.0 or higher
- API keys for your preferred LLM provider (OpenAI, Anthropic, etc.)

### Installation

#### Gradle (Kotlin DSL)

Add the following to your `build.gradle.kts` file:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("ai.kastrax:kastrax-core:0.1.0")
    implementation("ai.kastrax:kastrax-integrations:kastrax-openai:0.1.0") // Optional
}
```

#### Maven

```xml
<dependencies>
    <dependency>
        <groupId>ai.kastrax</groupId>
        <artifactId>kastrax-core</artifactId>
        <version>0.1.0</version>
    </dependency>
    <!-- Optional OpenAI integration -->
    <dependency>
        <groupId>ai.kastrax</groupId>
        <artifactId>kastrax-integrations-kastrax-openai</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
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

## Architecture

KastraX follows a layered architecture designed for flexibility, extensibility, and performance:

### Core Components

1. **Agent System**: The central component that orchestrates interactions between the user, LLMs, and tools. Agents can be created with a fluent Kotlin DSL, configured with various capabilities, and used to generate responses or stream them incrementally.

2. **LLM Abstraction Layer**: A unified interface for interacting with different Large Language Model providers. This layer handles prompt formatting, API communication, and response parsing, providing a consistent experience regardless of the underlying model.

3. **Tool System**: Enables agents to interact with external systems and services through a well-defined interface. Tools in KastraX use Zod schemas for input/output validation, ensuring type safety and proper error handling.

4. **Memory System**: Manages conversation history and context, supporting different types of memory including short-term, long-term, and working memory. The memory system provides intelligent context management for more coherent conversations.

5. **RAG System**: Implements Retrieval-Augmented Generation, allowing agents to retrieve relevant information from external sources to improve response quality. The RAG system includes document processing, embedding generation, and semantic search capabilities.

### Technical Implementation

KastraX leverages Kotlin's powerful features to create a developer-friendly framework:

- **Coroutines**: Asynchronous operations are implemented using Kotlin coroutines, providing non-blocking I/O without callback hell.
- **Type Safety**: Strong typing throughout the framework catches errors at compile time rather than runtime.
- **DSL Patterns**: Domain-specific languages make configuration and usage intuitive and concise.
- **Extension Functions**: Kotlin's extension functions allow for elegant API design and code organization.
- **Serialization**: Built-in support for kotlinx.serialization ensures efficient data handling.

### System Integration

KastraX is designed to integrate seamlessly with existing systems:

- **Server Deployment**: Modules for Spring Boot, Ktor, and Quarkus make it easy to deploy agents as web services.
- **Database Connectivity**: Direct integration with relational and NoSQL databases, with AI2DB capabilities for natural language querying.
- **Vector Stores**: Multiple vector database integrations for efficient embedding storage and retrieval.
- **Monitoring**: Built-in observability features for tracking agent performance and behavior.

## Project Structure

KastraX follows a modular architecture with the following components:

- **kastrax-core**: Core framework components ✅
- **kastrax-memory-api**: Memory system interfaces ✅
- **kastrax-memory-impl**: Memory system implementations ✅
- **kastrax-zod**: Schema validation system ✅
- **kastrax-integrations**: Third-party integrations
  - **kastrax-openai**: OpenAI integration ✅
  - **kastrax-deepseek**: DeepSeek integration ✅
  - **kastrax-anthropic**: Anthropic integration ✅
  - **kastrax-gemini**: Google Gemini integration ✅
- **kastrax-rag**: Retrieval-augmented generation ✅
- **kastrax-server**: Server components for hosting agents ✅
- **kastrax-cli**: Command-line tools ✅
- **kastrax-evals**: Evaluation framework ✅
- **kastrax-datasource**: Data source connectors ✅
- **kastrax-observability**: Monitoring and observability tools ✅
- **kastrax-a2a**: Agent-to-agent communication system ✅
- **kastrax-ai2db**: AI to database query generation ✅
- **kastrax-store**: Vector storage integrations ✅
  - **memory**: In-memory vector store ✅
  - **chroma**: Chroma integration ✅
  - **pinecone**: Pinecone integration ✅
  - **qdrant**: Qdrant integration ✅
  - **postgres**: PostgreSQL integration ✅
  - **lancedb**: LanceDB integration ✅
- **kastrax-runtime**: Runtime execution environment ✅
  - **kastrax-runtime-api**: Runtime API interfaces ✅
  - **kastrax-runtime-jvm**: JVM runtime implementation ✅

## Documentation

Detailed documentation is available in the `docs` directory and in the `kastrax-doc` module, which contains the source for our documentation website.

Visit our [Official Documentation](https://kastrax-doc.vercel.app/en/docs) for comprehensive guides and API references.

Key documentation topics include:

- Agent creation and configuration
- Tool building and integration
- LLM provider integrations
- Memory systems
- RAG implementation
- Agent-to-agent communication
- Schema validation with Zod
- Server deployment options
- Best practices and patterns

## Examples

The `examples` and `examples-modules` directories contain a variety of sample projects demonstrating different features of KastraX:

- Simple chat agents
- Tool usage examples
- RAG implementations
- Multi-agent systems
- Integration with databases
- Workflow automation

Each example includes detailed comments and explanations to help you understand how to use KastraX in different scenarios.

### Featured Examples

#### Basic Agent with Memory
```kotlin
val agent = agent {
    name = "HistoryAwareAssistant"
    instructions = "You are an assistant that remembers previous conversations."
    model = openAi("gpt-4o")
    
    memory {
        shortTerm {
            maxMessages = 10
        }
    }
}
```

#### RAG-enabled Search Agent
```kotlin
val searchAgent = agent {
    name = "DocSearcher"
    instructions = "You help users find information in documents."
    model = anthropic("claude-3-opus-20240229")
    
    rag {
        documentSource = filesystemSource("./docs")
        embedder = openAiEmbedder("text-embedding-3-large")
        vectorStore = chromaStore("search-index")
        chunkSize = 500
        chunkOverlap = 50
    }
}
```

## Advanced Use Cases

KastraX can be used to build a wide range of AI-powered applications:

### Multi-Agent Collaboration
```kotlin
// Create a team of specialized agents that work together
val researchAgent = agent { /* configuration */ }
val writerAgent = agent { /* configuration */ }
val editorAgent = agent { /* configuration */ }

// Connect agents using the A2A system
val workflow = agentWorkflow {
    step("research", researchAgent)
    step("write", writerAgent)
    step("edit", editorAgent)
    
    flow {
        "research" to "write"
        "write" to "edit"
    }
}
```

### Database Query Generation
```kotlin
// Create an AI2DB agent that translates natural language to SQL
val dbAgent = ai2dbAgent {
    databaseConnector {
        type = PostgreSQL
        url = "jdbc:postgresql://localhost:5432/mydatabase"
        // Authentication details
    }
}

val query = "Find all customers who purchased more than $1000 last month"
val result = dbAgent.query(query)
```

### Generative AI Applications

```kotlin
// Create a code-generating agent with specialized tools
val codeAgent = agent {
    name = "CodeAssistant"
    instructions = "You are a programming assistant that helps write and explain code."
    model = openAi("gpt-4o")
    
    tools {
        tool {
            id = "run_code"
            name = "CodeRunner"
            description = "Run code in a sandbox environment"
            
            input {
                field("language", Zod.string()) {
                    description = "The programming language (e.g. python, javascript)"
                }
                field("code", Zod.string()) {
                    description = "The code to execute"
                }
            }
            
            output {
                field("result", Zod.string()) {
                    description = "The execution result"
                }
                field("error", Zod.string().optional()) {
                    description = "Error message if execution failed"
                }
            }
            
            execute { input ->
                val language = input.get<String>("language")
                val code = input.get<String>("code")
                
                // Execute code safely in a sandbox
                val result = sandboxRunner.execute(language, code)
                
                mapOf(
                    "result" to result.output,
                    "error" to result.error
                )
            }
        }
    }
}
```

### Enterprise Integration

```kotlin
// Connect agents to enterprise systems
val enterpriseAgent = agent {
    name = "EnterpriseAssistant"
    instructions = "You help employees find information and perform tasks."
    model = openAi("gpt-4o")
    
    tools {
        // CRM Integration
        tool("salesforce_search") {
            // Tool definition
        }
        
        // ERP Integration
        tool("sap_query") {
            // Tool definition
        }
        
        // Internal Knowledge Base
        tool("confluence_search") {
            // Tool definition
        }
    }
    
    // Enterprise-grade security
    security {
        roleBasedAccess = true
        auditLogging = true
        sensitiveDataFiltering = true
    }
}
```

## Roadmap

- [x] Core agent framework
- [x] Multiple LLM provider integrations
- [x] Basic RAG capabilities
- [x] Vector store integrations
- [x] Agent-to-agent communication
- [x] Advanced agent reflection and planning
- [x] Browser automation tools
- [ ] Native mobile SDK
- [ ] Federated learning capabilities
- [ ] Enterprise security features

## Contributing

We welcome contributions from the community! Please see our [Contributing Guide](CONTRIBUTING.md) for details on how to get involved.

### Development Setup

1. Clone the repository
```bash
git clone https://github.com/kastrax-ai/kastrax.git
cd kastrax
```

2. Build the project
```bash
./gradlew build
```

3. Run the tests
```bash
./gradlew test
```

## Inspiration & Acknowledgments

KastraX is inspired by and builds upon several excellent projects in the AI agent ecosystem:

### Mastra Framework

We would like to acknowledge [Mastra](https://github.com/mas-3/mastra), an innovative TypeScript-based AI agent framework. KastraX draws significant inspiration from Mastra's architectural patterns and agent design philosophy, while adapting these concepts to leverage Kotlin's unique strengths. Our team conducted a thorough analysis of Mastra's structure and functionality, which greatly influenced KastraX's design.

### Augment Code

KastraX's programming assistant features were developed with reference to [Augment Code](https://github.com/augment-code), particularly its approach to IDE integration and code analysis. The kastrax-code module implements many similar capabilities, providing intelligent coding assistance powered by LLMs.

### Other Inspirations

- Kotlin's coroutines and DSL capabilities that enable our expressive API design
- The Actor model as implemented by the kactor library
- The open-source LLM ecosystem and its rapid advancement
- The broader AI research community and their contributions to agent architecture

## Community

- Join our [Discord server](https://discord.gg/kastrax) for discussions and support
- Follow us on [Twitter](https://twitter.com/kastraxai) for updates
- Check out our [Blog](https://blog.kastrax.ai) for tutorials and announcements

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
