package ai.kastrax.ragx.document

import ai.kastrax.store.document.Document

/**
 * 文档加载器接口，定义了加载文档的方法。
 */
interface DocumentLoader {
    /**
     * 加载文档。
     *
     * @return 加载的文档列表
     */
    suspend fun load(): List<Document>
}
