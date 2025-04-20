package ai.kastrax.fastembed.examples

import ai.kastrax.fastembed.EmbeddingModel
import ai.kastrax.fastembed.TextEmbedding

/**
 * A simple example demonstrating how to use the FastEmbed Kotlin library.
 */
fun main() {
    println("FastEmbed Kotlin Example")
    println("------------------------")

    // Create a text embedding model
    println("Creating model...")
    TextEmbedding.create(
        model = EmbeddingModel.BGE_SMALL_EN,
        showDownloadProgress = true
    ).use { model ->
        println("Model created with dimension: ${model.dimension}")

        // Generate embeddings for a list of texts
        val texts = listOf(
            "Hello, world!",
            "This is a test",
            "FastEmbed is awesome"
        )

        println("\nGenerating embeddings for ${texts.size} texts...")
        val embeddings = model.embed(texts)

        // Print information about the embeddings
        println("Generated ${embeddings.size} embeddings")
        embeddings.forEachIndexed { index, embedding ->
            println("  Embedding $index: dimension=${embedding.dimension}, first few values=${embedding.vector.take(5)}")
        }

        // Calculate similarity between two texts
        val text1 = "Hello, world!"
        val text2 = "Hi, world!"
        val text3 = "This is completely different."

        println("\nCalculating similarities...")
        val similarity1 = model.similarity(text1, text2)
        val similarity2 = model.similarity(text1, text3)

        println("  Similarity between '$text1' and '$text2': $similarity1")
        println("  Similarity between '$text1' and '$text3': $similarity2")
    }

    println("\nDone!")
}
