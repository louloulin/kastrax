# FastEmbed Kotlin Bindings

This project provides Kotlin bindings for the [fastembed-rs](https://github.com/Anush008/fastembed-rs) library, allowing you to generate text embeddings directly from Kotlin applications.

## Features

- Generate text embeddings using various models
- Support for batch processing and parallel processing
- Automatic model downloading and caching
- Native performance through JNI
- Coroutine support for asynchronous operations
- Vector store integration for similarity search
- RAG (Retrieval-Augmented Generation) support
- KastraX integration

## Installation

Add the dependency to your Gradle build file:

```kotlin
dependencies {
    implementation("ai.kastrax:fastembed-kotlin:0.1.0")
}
```

## Usage

### Basic Usage

```kotlin
import ai.kastrax.fastembed.TextEmbedding
import ai.kastrax.fastembed.EmbeddingModel

fun main() {
    // Create a text embedding model
    TextEmbedding.create(EmbeddingModel.BGE_SMALL_EN).use { model ->
        // Generate embeddings for a list of texts
        val texts = listOf(
            "Hello, world!",
            "This is a test",
            "FastEmbed is awesome"
        )

        val embeddings = model.embed(texts)

        // Print the dimensions of the first embedding
        println("Embedding dimensions: ${embeddings[0].dimension}")

        // Calculate similarity between two texts
        val similarity = model.similarity("Hello, world!", "Hi, world!")
        println("Similarity: $similarity")

        // Find similar texts
        val query = "What is FastEmbed?"
        val candidates = listOf(
            "FastEmbed is a library for generating embeddings",
            "This is an unrelated text",
            "Embeddings are vector representations of text"
        )

        val results = model.findSimilar(query, candidates, topK = 2)
        results.forEach { (index, score) ->
            println("${candidates[index]} (score: $score)")
        }
    }
}
```

### Asynchronous Usage with Coroutines

```kotlin
import ai.kastrax.fastembed.AsyncTextEmbedding
import ai.kastrax.fastembed.EmbeddingModel
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // Create an async text embedding model
    AsyncTextEmbedding.create(EmbeddingModel.BGE_SMALL_EN).use { model ->
        // Generate embeddings for a list of texts
        val texts = listOf(
            "Hello, world!",
            "This is a test",
            "FastEmbed is awesome"
        )

        // Process in parallel
        val embeddings = model.embedParallel(texts)

        // Calculate similarity between two texts
        val similarity = model.similarity("Hello, world!", "Hi, world!")
        println("Similarity: $similarity")
    }
}
```

### Vector Store Integration

```kotlin
import ai.kastrax.fastembed.TextEmbedding
import ai.kastrax.fastembed.EmbeddingModel
import ai.kastrax.fastembed.vectorstore.VectorStoreFactory

fun main() {
    TextEmbedding.create(EmbeddingModel.BGE_SMALL_EN).use { model ->
        // Create a vector store
        val vectorStore = VectorStoreFactory.createInMemoryStore(model)

        // Add documents
        val text = "FastEmbed is a library for generating embeddings"
        val embedding = model.embed(text)
        vectorStore.addItem("doc1", embedding, mapOf("text" to text))

        // Search for similar documents
        val query = "What is FastEmbed?"
        val queryEmbedding = model.embed(query)
        val results = vectorStore.search(queryEmbedding, topK = 5)

        results.forEach { result ->
            println("${result.metadata["text"]} (score: ${result.score})")
        }
    }
}
```

### RAG (Retrieval-Augmented Generation) Integration

```kotlin
import ai.kastrax.fastembed.TextEmbedding
import ai.kastrax.fastembed.EmbeddingModel
import ai.kastrax.fastembed.rag.SimpleRAG

fun main() {
    TextEmbedding.create(EmbeddingModel.BGE_SMALL_EN).use { model ->
        // Create a RAG system
        SimpleRAG(model).use { rag ->
            // Add documents
            rag.addDocument(
                "doc1",
                "FastEmbed is a library for generating vector embeddings.",
                mapOf("source" to "documentation")
            )

            rag.addDocument(
                "doc2",
                "Vector embeddings are numerical representations of data.",
                mapOf("source" to "documentation")
            )

            // Query the RAG system
            val query = "What is FastEmbed?"
            val results = rag.query(query, topK = 2)

            results.forEach { result ->
                println("${result.metadata["text"]} (score: ${result.score})")
            }
        }
    }
}
```

### KastraX Integration

```kotlin
import ai.kastrax.fastembed.EmbeddingModel
import ai.kastrax.fastembed.integration.KastraXEmbeddingService

fun main() {
    // Create a KastraX embedding service
    KastraXEmbeddingService.create(
        model = EmbeddingModel.BGE_SMALL_EN
    ).use { embeddingService ->
        // Generate embeddings
        val text = "Hello, world!"
        val embedding = embeddingService.embed(text)

        // Generate batch embeddings
        val texts = listOf("Hello, world!", "This is a test")
        val embeddings = embeddingService.embedBatch(texts)

        // Calculate similarity
        val similarity = embeddingService.similarity("Hello, world!", "Hi, world!")
        println("Similarity: $similarity")
    }
}
```

## Supported Models

The library supports the following embedding models:

- BGE Small EN (default)
- BGE Base EN
- BGE Small Chinese
- BGE Base Chinese
- MiniLM L6 V2
- MiniLM L6 V2 Q
- Multilingual E5 Small
- Multilingual E5 Large

## Building from Source

To build the library from source:

1. Install Rust and Cargo
2. Clone this repository
3. Run `./gradlew build`

## Dependencies

- Kotlin 1.9.x or higher
- Rust 1.70.0 or higher
- JDK 11 or higher
- Kotlinx Coroutines (for async API)

## Project Structure

- `src/main/kotlin/ai/kastrax/fastembed/` - Kotlin API
- `src/main/kotlin/ai/kastrax/fastembed/vectorstore/` - Vector store implementations
- `src/main/kotlin/ai/kastrax/fastembed/rag/` - RAG implementations
- `src/main/kotlin/ai/kastrax/fastembed/integration/` - KastraX integration
- `src/main/kotlin/ai/kastrax/fastembed/examples/` - Example code
- `rust/` - Rust JNI bindings

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.
