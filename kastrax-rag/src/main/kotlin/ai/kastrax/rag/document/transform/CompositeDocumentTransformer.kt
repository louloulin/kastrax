package ai.kastrax.rag.document.transform

import ai.kastrax.rag.document.Document
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 组合文档转换器，组合多个转换器。
 *
 * @property transformers 要组合的转换器列表
 */
class CompositeDocumentTransformer(
    private val transformers: List<DocumentTransformer>
) : DocumentTransformer {
    constructor(vararg transformers: DocumentTransformer) : this(transformers.toList())

    override fun transform(document: Document): Document {
        var transformedDocument = document

        for (transformer in transformers) {
            try {
                transformedDocument = transformer.transform(transformedDocument)
            } catch (e: Exception) {
                logger.error(e) { "Error applying transformer ${transformer.javaClass.simpleName} to document" }
            }
        }

        return transformedDocument
    }

    override fun transform(documents: List<Document>): List<Document> {
        var transformedDocuments = documents

        for (transformer in transformers) {
            try {
                transformedDocuments = transformer.transform(transformedDocuments)
            } catch (e: Exception) {
                logger.error(e) { "Error applying transformer ${transformer.javaClass.simpleName} to documents" }
            }
        }

        return transformedDocuments
    }
}

/**
 * 条件文档转换器，根据条件应用转换器。
 *
 * @property condition 条件函数
 * @property transformer 要应用的转换器
 * @property elseTransformer 条件不满足时要应用的转换器（可选）
 */
class ConditionalDocumentTransformer(
    private val condition: (Document) -> Boolean,
    private val transformer: DocumentTransformer,
    private val elseTransformer: DocumentTransformer? = null
) : DocumentTransformer {
    override fun transform(document: Document): Document {
        return if (condition(document)) {
            transformer.transform(document)
        } else {
            elseTransformer?.transform(document) ?: document
        }
    }

    override fun transform(documents: List<Document>): List<Document> {
        return documents.map { document ->
            transform(document)
        }
    }
}

/**
 * 并行文档转换器，并行应用多个转换器，然后合并结果。
 *
 * @property transformers 要并行应用的转换器列表
 * @property merger 合并函数
 */
class ParallelDocumentTransformer(
    private val transformers: List<DocumentTransformer>,
    private val merger: (List<Document>) -> Document
) : DocumentTransformer {
    constructor(
        vararg transformers: DocumentTransformer,
        merger: (List<Document>) -> Document
    ) : this(transformers.toList(), merger)

    override fun transform(document: Document): Document {
        val results = transformers.map { transformer ->
            try {
                transformer.transform(document)
            } catch (e: Exception) {
                logger.error(e) { "Error applying transformer ${transformer.javaClass.simpleName} to document" }
                document
            }
        }

        return merger(results)
    }

    /**
     * 默认的合并函数，连接所有文档的内容，合并所有元数据。
     */
    companion object {
        fun defaultMerger(documents: List<Document>): Document {
            if (documents.isEmpty()) {
                return Document("", emptyMap())
            }

            if (documents.size == 1) {
                return documents.first()
            }

            val content = documents.joinToString("\n\n") { it.content }

            val metadata = mutableMapOf<String, Any>()
            documents.forEachIndexed { index, document ->
                document.metadata.forEach { (key, value) ->
                    // 如果键已存在，添加索引后缀
                    val metadataKey = if (metadata.containsKey(key)) {
                        "${key}_${index}"
                    } else {
                        key
                    }
                    metadata[metadataKey] = value
                }
            }

            return Document(content, metadata)
        }
    }
}
