package ai.kastrax.fastembed.examples

import ai.kastrax.fastembed.EmbeddingModel
import ai.kastrax.fastembed.TextEmbedding
import java.io.File
import kotlin.math.min

/**
 * An example demonstrating how to integrate FastEmbed Kotlin with KastraX RAG.
 * 
 * Note: This is a simplified example that shows the integration pattern.
 * In a real application, you would use the actual KastraX RAG components.
 */
fun main() {
    println("FastEmbed KastraX RAG Integration Example")
    println("----------------------------------------")
    
    // Create a text embedding model
    println("Creating model...")
    TextEmbedding.create(
        model = EmbeddingModel.BGE_SMALL_EN,
        showDownloadProgress = true
    ).use { model ->
        println("Model created with dimension: ${model.dimension}")
        
        // Create a simple in-memory vector store
        val vectorStore = InMemoryVectorStore(model.dimension)
        
        // Create a document loader and splitter
        val documentLoader = SimpleDocumentLoader()
        val textSplitter = SimpleTextSplitter(chunkSize = 200, overlap = 50)
        
        // Load and process a document
        val documentPath = "sample_document.txt"
        val document = loadDocument(documentPath)
        val chunks = textSplitter.splitText(document)
        
        println("\nLoaded document: $documentPath")
        println("Created ${chunks.size} chunks")
        
        // Generate embeddings and add to vector store
        println("\nGenerating embeddings and adding to vector store...")
        chunks.forEachIndexed { index, chunk ->
            val embedding = model.embed(chunk)
            vectorStore.addItem(index.toString(), embedding, mapOf("text" to chunk))
        }
        
        // Simulate a query
        val query = "What is the main topic of this document?"
        println("\nQuery: $query")
        
        // Generate embedding for the query
        val queryEmbedding = model.embed(query)
        
        // Search the vector store
        val searchResults = vectorStore.search(queryEmbedding, topK = 3)
        
        // Print the results
        println("\nTop 3 most relevant chunks:")
        searchResults.forEach { result ->
            println("\nChunk ${result.id} (similarity: ${result.score}):")
            println("\"${result.metadata["text"]}\"")
        }
    }
    
    println("\nDone!")
}

/**
 * Load a document from a file.
 */
fun loadDocument1(path: String): String {
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
 * A simple document loader.
 */
class SimpleDocumentLoader {
    fun loadDocument(path: String): String {
        return File(path).readText()
    }
}

/**
 * A simple text splitter.
 */
class SimpleTextSplitter(private val chunkSize: Int, private val overlap: Int) {
    fun splitText(text: String): List<String> {
        val words = text.split(Regex("\\s+"))
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
}

/**
 * A simple in-memory vector store.
 */
class InMemoryVectorStore(private val dimensions: Int) {
    private val items = mutableListOf<VectorStoreItem>()
    
    fun addItem(id: String, embedding: ai.kastrax.fastembed.Embedding, metadata: Map<String, String>) {
        items.add(VectorStoreItem(id, embedding, metadata))
    }
    
    fun search(queryEmbedding: ai.kastrax.fastembed.Embedding, topK: Int): List<SearchResult> {
        return items.map { item ->
            val similarity = queryEmbedding.cosineSimilarity(item.embedding)
            SearchResult(item.id, similarity, item.metadata)
        }.sortedByDescending { it.score }.take(topK)
    }
    
    data class VectorStoreItem(
        val id: String,
        val embedding: ai.kastrax.fastembed.Embedding,
        val metadata: Map<String, String>
    )
    
    data class SearchResult(
        val id: String,
        val score: Float,
        val metadata: Map<String, String>
    )
}
