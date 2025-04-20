package ai.kastrax.fastembed.examples

import ai.kastrax.fastembed.AsyncTextEmbedding
import ai.kastrax.fastembed.EmbeddingModel
import ai.kastrax.fastembed.rag.AsyncSimpleRAG
import ai.kastrax.fastembed.util.format
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

/**
 * An example demonstrating how to use the AsyncSimpleRAG system.
 */
fun main() = runBlocking {
    println("FastEmbed Kotlin Async RAG Example")
    println("---------------------------------")

    // Create an async text embedding model
    println("Creating model...")
    AsyncTextEmbedding.create(
        model = EmbeddingModel.BGE_SMALL_EN,
        showDownloadProgress = true
    ).use { model ->
        println("Model created with dimension: ${model.dimension}")

        // Create an async RAG system
        AsyncSimpleRAG(model).use { rag ->
            // Add documents in parallel
            println("\nAdding documents in parallel...")

            val documents = listOf(
                Triple(
                    "doc1",
                    "FastEmbed is a library for generating vector embeddings from text or images.",
                    mapOf("source" to "documentation", "section" to "introduction")
                ),
                Triple(
                    "doc2",
                    "Vector embeddings are numerical representations of data that capture semantic meaning.",
                    mapOf("source" to "documentation", "section" to "concepts")
                ),
                Triple(
                    "doc3",
                    "FastEmbed provides a simple API for generating embeddings using state-of-the-art models.",
                    mapOf("source" to "documentation", "section" to "api")
                ),
                Triple(
                    "doc4",
                    "The library includes several pre-trained models, such as BGE and All-MiniLM-L6-v2.",
                    mapOf("source" to "documentation", "section" to "models")
                ),
                Triple(
                    "doc5",
                    "Kotlin is a modern programming language that makes developers happier.",
                    mapOf("source" to "blog", "section" to "languages")
                )
            )

            val count = rag.addDocuments(documents)
            println("Added $count documents")

            // Query the RAG system in parallel
            val queries = listOf(
                "What is FastEmbed?",
                "Tell me about vector embeddings",
                "What models are available?",
                "What is Kotlin?"
            )

            println("\nQuerying in parallel...")
            val deferredResults = queries.map { query ->
                async {
                    query to rag.query(query, topK = 2)
                }
            }

            val results = deferredResults.awaitAll()

            // Print results
            results.forEach { (query, queryResults) ->
                println("\nQuery: $query")
                println("Top results:")
                queryResults.forEachIndexed { index, result ->
                    println("${index + 1}. [Score: ${result.score.format(2)}] ${result.metadata["text"]}")
                    println("   Source: ${result.metadata["source"]}, Section: ${result.metadata["section"]}")
                }
            }
        }
    }

    println("\nDone!")
}


