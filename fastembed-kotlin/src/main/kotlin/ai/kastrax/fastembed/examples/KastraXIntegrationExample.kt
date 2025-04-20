package ai.kastrax.fastembed.examples

import ai.kastrax.fastembed.EmbeddingModel
import ai.kastrax.fastembed.integration.KastraXEmbeddingService
import ai.kastrax.fastembed.util.format
import ai.kastrax.fastembed.vectorstore.InMemoryVectorStore
import ai.kastrax.fastembed.vectorstore.SearchResult

/**
 * An example demonstrating how to integrate FastEmbed with KastraX.
 * This is a simplified example that simulates a KastraX RAG system.
 */
fun main() {
    println("FastEmbed KastraX Integration Example")
    println("------------------------------------")

    // Create a KastraX embedding service
    println("Creating embedding service...")
    KastraXEmbeddingService.create(
        model = EmbeddingModel.BGE_SMALL_EN,
        showDownloadProgress = true
    ).use { embeddingService ->
        println("Embedding service created with dimension: ${embeddingService.dimension}")

        // Create a vector store
        val vectorStore = InMemoryVectorStore(embeddingService.dimension)

        // Add documents
        println("\nAdding documents...")

        val documents = listOf(
            "FastEmbed is a library for generating vector embeddings from text or images.",
            "Vector embeddings are numerical representations of data that capture semantic meaning.",
            "FastEmbed provides a simple API for generating embeddings using state-of-the-art models.",
            "The library includes several pre-trained models, such as BGE and All-MiniLM-L6-v2.",
            "Kotlin is a modern programming language that makes developers happier."
        )

        // Generate embeddings for documents
        val embeddings = embeddingService.embedBatch(documents)

        // Add documents to vector store
        documents.zip(embeddings).forEachIndexed { index, (text, embedding) ->
            val id = "doc${index + 1}"
            val metadata = mapOf("text" to text)
            vectorStore.addItem(id, ai.kastrax.fastembed.Embedding(embedding), metadata)
        }

        println("Added ${vectorStore.count()} documents")

        // Query the vector store
        val queries = listOf(
            "What is FastEmbed?",
            "Tell me about vector embeddings",
            "What models are available?",
            "What is Kotlin?"
        )

        for (query in queries) {
            println("\nQuery: $query")

            // Generate embedding for query
            val queryEmbedding = embeddingService.embed(query)

            // Search vector store
            val results = vectorStore.search(
                ai.kastrax.fastembed.Embedding(queryEmbedding),
                topK = 2
            )

            // Print results
            println("Top results:")
            results.forEachIndexed { index, result ->
                println("${index + 1}. [Score: ${result.score.format(2)}] ${result.metadata["text"]}")
            }
        }

        // Calculate similarity between two texts
        val text1 = "Hello, world!"
        val text2 = "Hi, world!"
        val similarity = embeddingService.similarity(text1, text2)
        println("\nSimilarity between '$text1' and '$text2': ${similarity.format(4)}")

        // Clean up
        vectorStore.close()
    }

    println("\nDone!")
}


