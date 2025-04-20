package ai.kastrax.fastembed.examples

import ai.kastrax.fastembed.EmbeddingModel
import ai.kastrax.fastembed.TextEmbedding
import ai.kastrax.fastembed.util.format

/**
 * A simple example to test the FastEmbed Kotlin bindings.
 */
fun main() {
    println("FastEmbed Kotlin Simple Test Example")
    println("-----------------------------------")
    
    // Set test mode to true to use mock implementations
    System.setProperty("ai.kastrax.fastembed.test.mode", "true")
    
    // Create a text embedding model
    println("\nCreating model...")
    TextEmbedding.create(
        model = EmbeddingModel.BGE_SMALL_EN,
        showDownloadProgress = true
    ).use { model ->
        println("Model created with dimension: ${model.dimension}")
        
        // Generate an embedding for a single text
        val text = "Hello, world!"
        println("\nGenerating embedding for: \"$text\"")
        val embedding = model.embed(text)
        
        println("Embedding dimension: ${embedding.dimension}")
        println("First 5 values: ${embedding.vector.take(5).map { it.format(4) }}")
        
        // Generate embeddings for multiple texts
        val texts = listOf(
            "Hello, world!",
            "This is a test",
            "FastEmbed is awesome"
        )
        
        println("\nGenerating embeddings for ${texts.size} texts...")
        val embeddings = model.embed(texts)
        
        println("Generated ${embeddings.size} embeddings")
        embeddings.forEachIndexed { index, emb ->
            println("  Embedding $index: dimension=${emb.dimension}, first few values=${emb.vector.take(3).map { it.format(4) }}")
        }
        
        // Calculate similarity
        val text1 = "Hello, world!"
        val text2 = "Hi, world!"
        val text3 = "This is completely different."
        
        println("\nCalculating similarities...")
        val similarity1 = model.similarity(text1, text2)
        val similarity2 = model.similarity(text1, text3)
        
        println("Similarity between \"$text1\" and \"$text2\": ${similarity1.format(4)}")
        println("Similarity between \"$text1\" and \"$text3\": ${similarity2.format(4)}")
        
        // Find similar texts
        val query = "What is FastEmbed?"
        val candidates = listOf(
            "FastEmbed is a library for generating embeddings",
            "This is an unrelated text",
            "Embeddings are vector representations of text"
        )
        
        println("\nFinding similar texts to: \"$query\"")
        val results = model.findSimilar(query, candidates)
        
        println("Results:")
        results.forEachIndexed { index, (candidateIndex, score) ->
            println("  ${index + 1}. \"${candidates[candidateIndex]}\" (score: ${score.format(4)})")
        }
    }
    
    println("\nDone!")
}
