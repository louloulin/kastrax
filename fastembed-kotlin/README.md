# FastEmbed Kotlin Bindings

This project provides Kotlin bindings for the [fastembed-rs](https://github.com/Anush008/fastembed-rs) library, allowing you to generate text and image embeddings directly from Kotlin applications.

## Features

- Generate text embeddings using various models
- Support for batch processing
- Automatic model downloading and caching
- Native performance through JNI

## Installation

Add the dependency to your Gradle build file:

```kotlin
dependencies {
    implementation("ai.kastrax:fastembed-kotlin:0.1.0")
}
```

## Usage

```kotlin
import ai.kastrax.fastembed.TextEmbedding
import ai.kastrax.fastembed.EmbeddingModel

fun main() {
    // Create a text embedding model
    val model = TextEmbedding.create(EmbeddingModel.BGE_SMALL_EN)
    
    // Generate embeddings for a list of texts
    val texts = listOf(
        "Hello, world!",
        "This is a test",
        "FastEmbed is awesome"
    )
    
    val embeddings = model.embed(texts)
    
    // Print the dimensions of the first embedding
    println("Embedding dimensions: ${embeddings[0].size}")
    
    // Calculate similarity between two texts
    val similarity = model.similarity("Hello, world!", "Hi, world!")
    println("Similarity: $similarity")
    
    // Clean up resources
    model.close()
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

## License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.
