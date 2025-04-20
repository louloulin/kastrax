package ai.kastrax.fastembed.examples

import ai.kastrax.fastembed.EmbeddingModel
import ai.kastrax.fastembed.TextEmbedding
import ai.kastrax.fastembed.rag.SimpleRAG
import ai.kastrax.fastembed.util.format

/**
 * An example demonstrating how to use the SimpleRAG system.
 */
fun main() {
    println("FastEmbed Kotlin RAG Example")
    println("----------------------------")

    // Create a text embedding model
    println("Creating model...")
    TextEmbedding.create(
        model = EmbeddingModel.BGE_SMALL_EN,
        showDownloadProgress = true
    ).use { model ->
        println("Model created with dimension: ${model.dimension}")

        // Create a RAG system
        SimpleRAG(model).use { rag ->
            // Add documents
            println("\nAdding documents...")

            rag.addDocument(
                "doc1",
                "FastEmbed is a library for generating vector embeddings from text or images.",
                mapOf("source" to "documentation", "section" to "introduction")
            )

            rag.addDocument(
                "doc2",
                "Vector embeddings are numerical representations of data that capture semantic meaning.",
                mapOf("source" to "documentation", "section" to "concepts")
            )

            rag.addDocument(
                "doc3",
                "FastEmbed provides a simple API for generating embeddings using state-of-the-art models.",
                mapOf("source" to "documentation", "section" to "api")
            )

            rag.addDocument(
                "doc4",
                "The library includes several pre-trained models, such as BGE and All-MiniLM-L6-v2.",
                mapOf("source" to "documentation", "section" to "models")
            )

            rag.addDocument(
                "doc5",
                "Kotlin is a modern programming language that makes developers happier.",
                mapOf("source" to "blog", "section" to "languages")
            )

            println("Added ${rag.documentCount()} documents")

            // Query the RAG system
            val queries = listOf(
                "What is FastEmbed?",
                "Tell me about vector embeddings",
                "What models are available?",
                "What is Kotlin?"
            )

            for (query in queries) {
                println("\nQuery: $query")
                val results = rag.query(query, topK = 2)

                println("Top results:")
                results.forEachIndexed { index, result ->
                    println("${index + 1}. [Score: ${result.score.format(2)}] ${result.metadata["text"]}")
                    println("   Source: ${result.metadata["source"]}, Section: ${result.metadata["section"]}")
                }
            }
        }
    }

    println("\nDone!")
}


