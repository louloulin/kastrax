# FastEmbed Kotlin for KastraX

This module provides Kotlin bindings for the [fastembed-rs](https://github.com/Anush008/fastembed-rs) library, allowing you to generate text embeddings directly from Kotlin applications and integrate with KastraX.

## Features

- Generate text embeddings using various models
- Support for batch processing
- Automatic model downloading and caching
- Native performance through JNI
- Integration with KastraX RAG

## Installation

Add the dependency to your Gradle build file:

```kotlin
dependencies {
    implementation("ai.kastrax:fastembed-kotlin:0.1.0")
}
```

## Basic Usage

```kotlin
import ai.kastrax.fastembed.TextEmbedding
import ai.kastrax.fastembed.EmbeddingModel

fun main() {
    // Create a text embedding model
    TextEmbedding.create(EmbeddingModel.BGE_SMALL_EN).use { model ->
        // Generate an embedding for a single text
        val text = "Hello, world!"
        val embedding = model.embed(text)
        
        println("Embedding dimension: ${embedding.dimension}")
        
        // Generate embeddings for multiple texts
        val texts = listOf(
            "Hello, world!",
            "This is a test",
            "FastEmbed is awesome"
        )
        
        val embeddings = model.embed(texts)
        
        // Calculate similarity between two texts
        val similarity = model.similarity("Hello, world!", "Hi, world!")
        println("Similarity: $similarity")
    }
}
```

## Integration with KastraX RAG

FastEmbed Kotlin can be integrated with KastraX RAG using the `FastEmbedEmbeddingService` class:

```kotlin
import ai.kastrax.fastembed.EmbeddingModel
import ai.kastrax.fastembed.integration.FastEmbedEmbeddingService
import ai.kastrax.rag.RAG
import ai.kastrax.rag.vectorstore.InMemoryVectorStore

// Create a vector store
val vectorStore = InMemoryVectorStore()

// Create a FastEmbed embedding service
val embeddingService = FastEmbedEmbeddingService.create(
    model = EmbeddingModel.BGE_SMALL_ZH,  // Chinese small model
    showDownloadProgress = true
)

// Create a RAG system
val rag = RAG(vectorStore, embeddingService)

// Add documents to the RAG system
rag.addDocument("FastEmbed is a library for generating vector embeddings.")
rag.addDocument("Vector embeddings are numerical representations of data.")

// Query the RAG system
val results = rag.query("What is FastEmbed?")
println(results)
```

## Supported Models

The library supports the following embedding models:

- `BGE_SMALL_EN` - BGE Small English (384 dimensions)
- `BGE_BASE_EN` - BGE Base English (768 dimensions)
- `BGE_SMALL_ZH` - BGE Small Chinese (384 dimensions)
- `BGE_BASE_ZH` - BGE Base Chinese (768 dimensions)
- `ALL_MINILM_L6_V2` - All-MiniLM-L6-v2 (384 dimensions)
- `ALL_MINILM_L6_V2_Q` - All-MiniLM-L6-v2 Quantized (384 dimensions)
- `E5_SMALL_V2` - E5 Small v2 (384 dimensions)
- `E5_LARGE_V2` - E5 Large v2 (1024 dimensions)

## Building from Source

To build the library from source:

1. Install Rust and Cargo
2. Clone this repository
3. Run `./gradlew build`

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.
