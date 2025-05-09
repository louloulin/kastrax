package ai.kastrax.rag.document

import ai.kastrax.store.document.Document

/**
 * 文档分割器接口，定义了分割文档的方法。
 */
interface DocumentSplitter {
    /**
     * 分割文档。
     *
     * @param document 文档
     * @return 分割后的文档列表
     */
    fun split(document: Document): List<Document>
}
