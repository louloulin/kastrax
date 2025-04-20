package ai.kastrax.fastembed.examples

import ai.kastrax.fastembed.EmbeddingModel
import ai.kastrax.fastembed.TextEmbedding
import java.io.File
import kotlin.math.min

/**
 * A simple example demonstrating how to use the FastEmbed Kotlin library for a basic RAG system.
 */
fun main() {
    println("FastEmbed Simple RAG Example")
    println("----------------------------")

    // Create a text embedding model
    println("Creating model...")
    TextEmbedding.create(
        model = EmbeddingModel.BGE_SMALL_EN,
        showDownloadProgress = true
    ).use { model ->
        println("Model created with dimension: ${model.dimension}")

        // Load and chunk a document
        val documentPath = "sample_document.txt"
        val document = loadDocument(documentPath)
        val chunks = chunkDocument(document, chunkSize = 200, overlap = 50)

        println("\nLoaded document: $documentPath")
        println("Created ${chunks.size} chunks")

        // Generate embeddings for all chunks
        println("\nGenerating embeddings for chunks...")
        val chunkEmbeddings = model.embed(chunks)
        println("Generated ${chunkEmbeddings.size} embeddings")

        // Simulate a query
        val query = "What is the main topic of this document?"
        println("\nQuery: $query")

        // Generate embedding for the query
        val queryEmbedding = model.embed(query)

        // Find the most similar chunks using the findSimilar method
        val similarities = model.findSimilar(queryEmbedding, chunkEmbeddings)

        // Print the top 3 most similar chunks
        println("\nTop 3 most relevant chunks:")
        similarities.take(3).forEach { (index, similarity) ->
            println("\nChunk $index (similarity: $similarity):")
            println("\"${chunks[index]}\"")
        }
    }

    println("\nDone!")
}

/**
 * Load a document from a file.
 */
fun loadDocument(path: String): String {
    // For this example, we'll create a sample document if it doesn't exist
    val file = File(path)
    if (!file.exists()) {
        file.writeText("""
            FastEmbed is a library for generating vector embeddings from text or images.

            Vector embeddings are numerical representations of data that capture semantic meaning,
            allowing for efficient similarity comparisons. These embeddings are fundamental to
            many modern AI applications, including search, recommendation systems, and
            natural language processing.

            FastEmbed provides a simple API for generating embeddings using state-of-the-art models.
            It supports multiple languages and is optimized for performance, making it suitable for
            both development and production environments.

            The library includes several pre-trained models, such as BGE (BAAI General Embeddings),
            All-MiniLM-L6-v2, and E5, each with different characteristics and performance profiles.

            FastEmbed is available in multiple programming languages, including Rust, Python, Go,
            JavaScript, and now Kotlin, making it accessible to a wide range of developers.
        """.trimIndent())
    }

    return file.readText()
}

/**
 * Split a document into chunks with overlap.
 */
fun chunkDocument(document: String, chunkSize: Int, overlap: Int): List<String> {
    val words = document.split(Regex("\\s+"))
    val chunks = mutableListOf<String>()

    var i = 0
    while (i < words.size) {
        val end = min(i + chunkSize, words.size)
        val chunk = words.subList(i, end).joinToString(" ")
        chunks.add(chunk)

        i += chunkSize - overlap
    }

    return chunks
}
