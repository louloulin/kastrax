package ai.kastrax.rag.adapter

import ai.kastrax.rag.document.RagDocument
import ai.kastrax.rag.model.SearchResult as RagSearchResult
import ai.kastrax.store.model.SearchResult as StoreSearchResult
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult

/**
 * 存储适配器，用于将 kastrax-store 模块中的类适配到 kastrax-rag 模块中的类。
 */
object StoreAdapter {

    /**
     * 将 kastrax-store 模块中的 SearchResult 类转换为 kastrax-rag 模块中的 SearchResult 类。
     *
     * @param storeResult kastrax-store 模块中的 SearchResult 类
     * @return kastrax-rag 模块中的 SearchResult 类
     */
    fun toRagSearchResult(storeResult: StoreSearchResult): RagSearchResult {
        val content = storeResult.metadata?.get("content") as? String ?: ""
        val metadata = storeResult.metadata?.filterKeys { it != "content" } ?: emptyMap()
        
        return RagSearchResult(
            id = storeResult.id,
            content = content,
            score = storeResult.score,
            metadata = metadata
        )
    }

    /**
     * 将 kastrax-store 模块中的 DocumentSearchResult 类转换为 kastrax-rag 模块中的 SearchResult 类。
     *
     * @param documentResult kastrax-store 模块中的 DocumentSearchResult 类
     * @return kastrax-rag 模块中的 SearchResult 类
     */
    fun toRagSearchResult(documentResult: DocumentSearchResult): RagSearchResult {
        return RagSearchResult(
            id = documentResult.document.id,
            content = documentResult.document.content,
            score = documentResult.score,
            metadata = documentResult.document.metadata
        )
    }

    /**
     * 将 kastrax-rag 模块中的 RagDocument 类转换为 kastrax-store 模块中的 Document 类。
     *
     * @param ragDocument kastrax-rag 模块中的 RagDocument 类
     * @return kastrax-store 模块中的 Document 类
     */
    fun toStoreDocument(ragDocument: RagDocument): Document {
        return Document(
            id = ragDocument.id,
            content = ragDocument.content,
            metadata = ragDocument.metadata
        )
    }

    /**
     * 将 kastrax-store 模块中的 Document 类转换为 kastrax-rag 模块中的 RagDocument 类。
     *
     * @param document kastrax-store 模块中的 Document 类
     * @return kastrax-rag 模块中的 RagDocument 类
     */
    fun toRagDocument(document: Document): RagDocument {
        return RagDocument(
            id = document.id,
            content = document.content,
            metadata = document.metadata,
            embedding = null
        )
    }

    /**
     * 将 kastrax-rag 模块中的 SearchResult 类转换为 kastrax-store 模块中的 SearchResult 类。
     *
     * @param ragResult kastrax-rag 模块中的 SearchResult 类
     * @return kastrax-store 模块中的 SearchResult 类
     */
    fun toStoreSearchResult(ragResult: RagSearchResult): StoreSearchResult {
        return StoreSearchResult(
            id = ragResult.id,
            score = ragResult.score,
            vector = null,
            metadata = ragResult.metadata + ("content" to ragResult.content)
        )
    }

    /**
     * 将 kastrax-rag 模块中的 SearchResult 类列表转换为 kastrax-store 模块中的 SearchResult 类列表。
     *
     * @param ragResults kastrax-rag 模块中的 SearchResult 类列表
     * @return kastrax-store 模块中的 SearchResult 类列表
     */
    fun toStoreSearchResults(ragResults: List<RagSearchResult>): List<StoreSearchResult> {
        return ragResults.map { toStoreSearchResult(it) }
    }

    /**
     * 将 kastrax-store 模块中的 SearchResult 类列表转换为 kastrax-rag 模块中的 SearchResult 类列表。
     *
     * @param storeResults kastrax-store 模块中的 SearchResult 类列表
     * @return kastrax-rag 模块中的 SearchResult 类列表
     */
    fun toRagSearchResults(storeResults: List<StoreSearchResult>): List<RagSearchResult> {
        return storeResults.map { toRagSearchResult(it) }
    }

    /**
     * 将 kastrax-store 模块中的 DocumentSearchResult 类列表转换为 kastrax-rag 模块中的 SearchResult 类列表。
     *
     * @param documentResults kastrax-store 模块中的 DocumentSearchResult 类列表
     * @return kastrax-rag 模块中的 SearchResult 类列表
     */
    fun toRagSearchResults(documentResults: List<DocumentSearchResult>): List<RagSearchResult> {
        return documentResults.map { toRagSearchResult(it) }
    }
}
