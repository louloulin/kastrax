package ai.kastrax.rag.vectorstore

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 兼容旧版本的内存向量存储实现。
 * 
 * 这个类是为了向后兼容而保留的，新代码应该直接使用 InMemoryVectorStore。
 */
class RagInMemoryVectorStore : InMemoryVectorStore() {
    init {
        logger.warn { "RagInMemoryVectorStore is deprecated. Use InMemoryVectorStore instead." }
    }
}
