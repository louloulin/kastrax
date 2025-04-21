package ai.kastrax.rag.vectorstore

import ai.kastrax.rag.document.Document
import ai.kastrax.rag.embedding.Embedding
import ai.kastrax.rag.embedding.EmbeddedDocument
import ai.kastrax.rag.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

// SearchResult moved to RagVectorStore.kt

// RagVectorStore interface moved to RagVectorStore.kt

// InMemoryVectorStore moved to InMemoryVectorStore.kt
