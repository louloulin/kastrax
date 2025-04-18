# KastraX: Modern AI Agent Framework

> 注意：中文文档请参见 [docs/kastrax_zh.md](docs/kastrax_zh.md)
>
> 中文文档目录：
> - [快速入门指南](docs/quickstart_zh.md)
> - [代理系统详解](docs/agents_zh.md)
> - [工具系统详解](docs/tools_zh.md)
> - [LLM 抽象层详解](docs/llm_abstraction_zh.md)
> - [内存系统详解](docs/memory_zh.md)
> - [Mastra 分析与 KastraX 设计](docs/mastra_analysis_zh.md)
> - [中文 README](README_zh.md)

## Introduction

KastraX is a modern AI Agent framework built in Kotlin, inspired by Mastra and Kastra. It provides a comprehensive set of tools and abstractions for building AI-powered applications with a focus on type safety, modularity, and developer experience.

## Core Design Principles

1. **Kotlin-first**: Leverage Kotlin's language features (coroutines, DSL, extension functions) for a fluid development experience
2. **Type safety**: Strong typing throughout the framework, catching errors at compile time
3. **Modularity**: Designed with a modular architecture and clear separation of concerns
4. **Extensibility**: Easy to extend and customize the framework for specific use cases
5. **Performance**: Optimized for performance and resource efficiency
6. **Developer experience**: Intuitive API and comprehensive documentation

## Project Structure

KastraX follows a modular architecture with the following components:

```
kastrax/
├── kastrax-core/               # Core framework components ✅
├── kastrax-memory-api/         # Memory system interfaces ✅
├── kastrax-memory-impl/        # Memory system implementations ✅
├── kastrax-rag/                # Retrieval-augmented generation
├── kastrax-cli/                # Command-line tools
├── kastrax-evals/              # Evaluation framework
├── kastrax-deployer/           # Deployment tools
├── kastrax-voice/              # Voice capabilities
├── kastrax-integrations/       # Third-party integrations
│   ├── kastrax-openai/         # OpenAI integration ✅
│   ├── kastrax-anthropic/      # Anthropic integration
│   ├── kastrax-gemini/         # Google Gemini integration
│   └── kastrax-mistral/        # Mistral integration
└── examples/                   # Example applications ✅
```

## Mastra Analysis and KastraX Design

After analyzing the Mastra codebase, we've designed KastraX to incorporate the best aspects of Mastra while leveraging Kotlin's strengths. Here's a detailed analysis of Mastra's architecture and how it translates to KastraX:

### Core Components

#### 1. Agent System

**Mastra Implementation:**
- Agents in Mastra are defined as classes that encapsulate LLM interactions
- They support tools, memory, and can be composed into workflows
- Agents have a flexible configuration system for customizing behavior

**KastraX Design:**
- Implement a Kotlin DSL for agent definition, making it more concise and type-safe
- Use Kotlin's extension functions for enhanced composability
- Leverage coroutines for asynchronous operations
- Provide stronger type checking for agent configurations

```kotlin
// KastraX Agent DSL
val myAgent = agent {
    name = "Assistant"
    instructions = "You are a helpful assistant."
    model = openAi("gpt-4o")

    tools {
        tool(calculatorTool)
        tool(weatherTool)
    }

    memory = memory {
        storage(inMemoryStorage())
        lastMessages(10)
        semanticRecall(true)
    }
}
```

#### 2. LLM Abstraction

**Mastra Implementation:**
- Provides a unified interface for different LLM providers
- Supports streaming responses and tool calling
- Includes message formatting and response parsing

**KastraX Design:**
- Create a provider-agnostic interface with Kotlin's sealed classes for better type safety
- Use Kotlin's Flow for streaming responses
- Implement extension functions for common operations
- Provide DSL for message construction

```kotlin
interface LlmProvider {
    val model: String

    suspend fun generate(
        messages: List<LlmMessage>,
        options: LlmOptions = LlmOptions()
    ): LlmResponse

    suspend fun streamGenerate(
        messages: List<LlmMessage>,
        options: LlmOptions = LlmOptions()
    ): Flow<String>

    suspend fun embedText(text: String): List<Float>
}
```

#### 3. Memory System

**Mastra Implementation:**
- Provides interfaces for storing and retrieving conversation history
- Supports different storage backends
- Includes thread management for multiple conversations

**KastraX Design:**
- Split into API and implementation modules for better separation of concerns
- Use Kotlin's coroutines for asynchronous storage operations
- Implement thread-safe storage with Mutex
- Provide DSL for memory configuration

```kotlin
interface Memory {
    suspend fun saveMessage(message: Message, threadId: String): String
    suspend fun getMessages(threadId: String, limit: Int = 10): List<MemoryMessage>
    suspend fun searchMessages(query: String, threadId: String, limit: Int = 5): List<MemoryMessage>
    suspend fun createThread(title: String? = null): String
    suspend fun deleteThread(threadId: String): Boolean
    suspend fun getThread(threadId: String): MemoryThread?
    suspend fun listThreads(limit: Int = 20, offset: Int = 0): List<MemoryThread>
}
```

#### 4. Tool System

**Mastra Implementation:**
- Tools are defined with input/output schemas and execution logic
- Supports validation of inputs and outputs
- Includes error handling and retry mechanisms

**KastraX Design:**
- Use Kotlin's DSL for tool definition
- Leverage Kotlin's type system for input/output validation
- Implement coroutine-based execution for better performance
- Provide extension functions for common tool operations

```kotlin
val calculatorTool = tool {
    id = "calculator"
    name = "Calculator"
    description = "Perform mathematical calculations"

    inputSchema = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("expression") {
                put("type", "string")
                put("description", "The mathematical expression to evaluate")
            }
        }
        putJsonArray("required") {
            add("expression")
        }
    }

    outputSchema = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("result") {
                put("type", "number")
                put("description", "The result of the calculation")
            }
        }
    }

    execute = { input ->
        val expression = input.jsonObject["expression"]?.jsonPrimitive?.content ?: "0"
        val result = evaluateExpression(expression)
        buildJsonObject {
            put("result", result)
        }
    }
}
```

#### 5. Workflow System

**Mastra Implementation:**
- Workflows in Mastra are defined as a series of steps
- Supports conditional branching, loops, and parallel execution
- Includes state management and persistence

**KastraX Design:**
- Implement a Kotlin DSL for workflow definition
- Use Kotlin's coroutines for step execution
- Leverage Kotlin's Flow for workflow state updates
- Provide stronger type checking for workflow configurations

```kotlin
val myWorkflow = workflow {
    name = "content-creation"

    step(researchAgent) {
        id = "research"
        variables = mapOf(
            "topic" to variable("$.input.topic")
        )
    }

    step(writingAgent) {
        id = "writing"
        after("research")
        variables = mapOf(
            "research" to variable("$.steps.research.output")
        )
    }

    step(editingAgent) {
        id = "editing"
        after("writing")
        variables = mapOf(
            "draft" to variable("$.steps.writing.output.text")
        )
    }
}
```

### API Design Comparison

#### Agent API

**Mastra:**
```typescript
const agent = new Agent({
  name: "assistant",
  llm: new OpenAI({ model: "gpt-4" }),
  tools: [calculatorTool, weatherTool],
  memory: new Memory()
});

const response = await agent.generate("What's the weather like?");
```

**KastraX:**
```kotlin
val agent = agent {
    name = "assistant"
    model = openAi("gpt-4o")
    tools {
        tool(calculatorTool)
        tool(weatherTool)
    }
    memory = memory {
        storage(inMemoryStorage())
    }
}

val response = agent.generate("What's the weather like?")
```

#### Tool API

**Mastra:**
```typescript
const calculatorTool = {
  id: "calculator",
  name: "Calculator",
  description: "Perform mathematical calculations",
  inputSchema: z.object({
    expression: z.string().describe("The mathematical expression to evaluate")
  }),
  outputSchema: z.object({
    result: z.number().describe("The result of the calculation")
  }),
  execute: async ({ expression }) => {
    const result = evaluateExpression(expression);
    return { result };
  }
};
```

**KastraX:**
```kotlin
val calculatorTool = tool {
    id = "calculator"
    name = "Calculator"
    description = "Perform mathematical calculations"

    inputSchema = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("expression") {
                put("type", "string")
                put("description", "The mathematical expression to evaluate")
            }
        }
        putJsonArray("required") {
            add("expression")
        }
    }

    outputSchema = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("result") {
                put("type", "number")
                put("description", "The result of the calculation")
            }
        }
    }

    execute = { input ->
        val expression = input.jsonObject["expression"]?.jsonPrimitive?.content ?: "0"
        val result = evaluateExpression(expression)
        buildJsonObject {
            put("result", result)
        }
    }
}
```

#### Workflow API

**Mastra:**
```typescript
const workflow = new Workflow({
  name: "content-creation",
  steps: [
    {
      id: "research",
      agent: researchAgent,
      variables: {
        topic: { ref: "$.input.topic" }
      }
    },
    {
      id: "writing",
      agent: writingAgent,
      after: ["research"],
      variables: {
        research: { ref: "$.steps.research.output" }
      }
    }
  ]
});

const result = await workflow.run({ topic: "AI" });
```

**KastraX:**
```kotlin
val workflow = workflow {
    name = "content-creation"

    step(researchAgent) {
        id = "research"
        variables = mapOf(
            "topic" to variable("$.input.topic")
        )
    }

    step(writingAgent) {
        id = "writing"
        after("research")
        variables = mapOf(
            "research" to variable("$.steps.research.output")
        )
    }
}

val result = workflow.execute(mapOf("topic" to "AI"))
```

## Implementation Plan

### Phase 1: Core Framework (Priority: High)

1. **kastrax-core** ✅
   - Implement base interfaces and abstractions ✅
   - Create Agent system with DSL ✅
   - Develop Tool system ✅
   - Build LLM abstraction layer ✅
   - Implement Workflow engine ⏳

2. **kastrax-memory-api** ✅
   - Design memory interfaces ✅
   - Define message and thread models ✅
   - Create memory builder interface ✅

3. **kastrax-memory-impl** ✅
   - Implement in-memory storage ✅
   - Create persistence layer ✅
   - Add semantic search capabilities ✅

4. **kastrax-integrations**
   - Implement OpenAI integration ✅
   - Implement Anthropic integration ⏳
   - Implement Google Gemini integration ⏳
   - Implement Mistral integration ⏳

### Phase 2: Advanced Features (Priority: Medium)

1. **kastrax-rag**
   - Implement document processing
   - Create embedding service
   - Build vector storage integrations
   - Develop reranking strategies

2. **kastrax-evals**
   - Design evaluation framework
   - Implement common evaluators
   - Create evaluation reporting

3. **kastrax-cli**
   - Develop project scaffolding
   - Create interactive playground
   - Implement deployment commands

### Phase 3: Specialized Components (Priority: Low)

1. **kastrax-deployer**
   - Implement serverless deployment
   - Create container deployment
   - Build API gateway

2. **kastrax-voice**
   - Implement text-to-speech
   - Create speech-to-text
   - Build voice agent interfaces

## Comparison with Mastra and Kastra

### Improvements over Mastra

1. **Enhanced DSL**: More intuitive and comprehensive DSL for defining agents, tools, and workflows
2. **Better type safety**: Stronger type checking and inference throughout the framework
3. **Improved memory system**: More flexible and powerful memory system with better semantic search
4. **Advanced RAG**: Comprehensive RAG system with document processing, embeddings, and reranking
5. **Workflow engine**: More powerful workflow engine with better error handling and visualization

### Improvements over Kastra

1. **Kotlin advantages**: Leveraging Kotlin's language features for better concurrency and type safety
2. **Simplified API**: More consistent and intuitive API design
3. **Better integration**: Tighter integration between components
4. **Performance**: Better performance through Kotlin's efficient coroutines
5. **JVM ecosystem**: Access to the rich ecosystem of JVM libraries and tools

## To-Do List (by priority)

### High Priority

1. Set up project structure and build system ✅
2. Implement core interfaces and abstractions ✅
3. Create LLM provider abstraction and implementations ✅
4. Develop Agent system with DSL ✅
5. Implement Tool system ✅
6. Create basic Memory system ✅
7. Develop simple Workflow engine ⏳
8. Write comprehensive tests for core components ✅
9. Create documentation for core features ✅

### Medium Priority

1. Enhance Memory system with semantic search ✅
2. Implement RAG system
3. Develop evaluation framework
4. Create CLI tool for project management
5. Implement more LLM provider integrations ⏳
6. Add advanced workflow features
7. Create example applications ✅
8. Improve error handling and telemetry

### Low Priority

1. Implement deployment tools
2. Develop voice capabilities
3. Create visualization tools for workflows
4. Add more specialized tools and integrations
5. Implement advanced RAG features
6. Create performance optimization tools
7. Develop monitoring and analytics
8. Build community and contribution guidelines

## Conclusion

KastraX combines the best aspects of Kastra and Mastra into a modern, powerful, and developer-friendly AI agent framework. By leveraging Kotlin's language features and the JVM ecosystem, KastraX provides a solid foundation for building complex AI applications with a focus on type safety, modularity, and performance.

The modular architecture allows developers to use only the components they need, while the comprehensive DSL makes it easy to create and configure agents, tools, and workflows. With strong typing throughout the framework, developers can catch errors at compile time and get better IDE support.

KastraX is designed to be extensible, allowing developers to customize and extend the framework for specific use cases. Comprehensive documentation and examples make it easy to get started and learn how to use the framework effectively.
