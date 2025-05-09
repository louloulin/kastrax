package ai.kastrax.rag.document.transform

import ai.kastrax.store.document.Document

/**
 * 文档转换器接口，定义了转换文档的方法。
 */
interface DocumentTransformer {
    /**
     * 转换文档。
     *
     * @param document 文档
     * @return 转换后的文档
     */
    fun transform(document: Document): Document
    
    /**
     * 转换文档列表。
     *
     * @param documents 文档列表
     * @return 转换后的文档列表
     */
    fun transformBatch(documents: List<Document>): List<Document> {
        return documents.map { transform(it) }
    }
}
