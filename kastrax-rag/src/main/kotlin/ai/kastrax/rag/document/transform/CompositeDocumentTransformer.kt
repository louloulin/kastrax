package ai.kastrax.rag.document.transform

import ai.kastrax.store.document.Document

/**
 * 组合文档转换器，将多个文档转换器组合在一起。
 *
 * @property transformers 文档转换器列表
 */
class CompositeDocumentTransformer(
    private val transformers: List<DocumentTransformer>
) : DocumentTransformer {
    /**
     * 构造函数。
     *
     * @param vararg transformers 文档转换器
     */
    constructor(vararg transformers: DocumentTransformer) : this(transformers.toList())
    
    /**
     * 转换文档。
     *
     * @param document 文档
     * @return 转换后的文档
     */
    override fun transform(document: Document): Document {
        var result = document
        for (transformer in transformers) {
            result = transformer.transform(result)
        }
        return result
    }
    
    /**
     * 转换文档列表。
     *
     * @param documents 文档列表
     * @return 转换后的文档列表
     */
    override fun transformBatch(documents: List<Document>): List<Document> {
        var results = documents
        for (transformer in transformers) {
            results = transformer.transformBatch(results)
        }
        return results
    }
}
