package ai.kastrax.rag.document.transform

import ai.kastrax.rag.document.Document

/**
 * 文档转换器接口，用于转换文档。
 */
interface DocumentTransformer {
    /**
     * 转换文档。
     *
     * @param document 要转换的文档
     * @return 转换后的文档
     */
    fun transform(document: Document): Document
    
    /**
     * 转换多个文档。
     *
     * @param documents 要转换的文档列表
     * @return 转换后的文档列表
     */
    fun transform(documents: List<Document>): List<Document> {
        return documents.map { transform(it) }
    }
}

/**
 * 文本清理转换器，使用 TextCleaner 清理文档文本。
 *
 * @property cleaner 文本清理器
 * @property options 清理选项
 */
class TextCleaningTransformer(
    private val cleaner: TextCleaner = TextCleaner(),
    private val options: TextCleaner.CleaningOptions = TextCleaner.CleaningOptions()
) : DocumentTransformer {
    
    override fun transform(document: Document): Document {
        return cleaner.clean(document, options)
    }
}

/**
 * HTML 到文本转换器，使用 HtmlToTextConverter 将 HTML 文档转换为纯文本。
 *
 * @property converter HTML 到文本转换器
 * @property options 转换选项
 */
class HtmlToTextTransformer(
    private val converter: HtmlToTextConverter = HtmlToTextConverter(),
    private val options: HtmlToTextConverter.ConversionOptions = HtmlToTextConverter.ConversionOptions()
) : DocumentTransformer {
    
    override fun transform(document: Document): Document {
        return converter.convert(document, options)
    }
}

/**
 * 表格提取转换器，使用 TableExtractor 从文档中提取表格。
 *
 * @property extractor 表格提取器
 * @property options 提取选项
 * @property outputFormat 输出格式，可选值为 "csv"、"markdown"、"json" 或 "html"
 * @property extractAsDocuments 是否将每个表格提取为单独的文档
 */
class TableExtractionTransformer(
    private val extractor: TableExtractor = TableExtractor(),
    private val options: TableExtractor.ExtractionOptions = TableExtractor.ExtractionOptions(),
    private val outputFormat: String = "markdown",
    private val extractAsDocuments: Boolean = true
) : DocumentTransformer {
    
    override fun transform(document: Document): Document {
        val tables = extractor.extract(document, options)
        
        return if (extractAsDocuments) {
            // 如果没有表格，返回原始文档
            if (tables.isEmpty()) {
                document
            } else {
                // 将第一个表格转换为文档并返回
                tables.first().toDocument(outputFormat)
            }
        } else {
            // 将所有表格合并为一个文档
            val content = tables.joinToString("\n\n") { 
                when (outputFormat.lowercase()) {
                    "csv" -> it.toCsv()
                    "markdown" -> it.toMarkdown()
                    "json" -> it.toJson(true)
                    "html" -> it.toHtml()
                    else -> it.toMarkdown()
                }
            }
            
            val metadata = document.metadata.toMutableMap().apply {
                put("table_count", tables.size)
                put("table_format", outputFormat)
                tables.forEachIndexed { index, table ->
                    put("table_${index}_id", table.id)
                    put("table_${index}_name", table.name)
                    put("table_${index}_source", table.source)
                    put("table_${index}_headers", table.headers)
                    put("table_${index}_row_count", table.rows.size)
                }
            }
            
            Document(content, metadata)
        }
    }
    
    override fun transform(documents: List<Document>): List<Document> {
        return if (extractAsDocuments) {
            // 从每个文档中提取表格，并将每个表格转换为单独的文档
            documents.flatMap { document ->
                val tables = extractor.extract(document, options)
                if (tables.isEmpty()) {
                    listOf(document)
                } else {
                    tables.map { it.toDocument(outputFormat) }
                }
            }
        } else {
            // 对每个文档单独处理
            documents.map { transform(it) }
        }
    }
}

/**
 * 复合转换器，按顺序应用多个转换器。
 *
 * @property transformers 要应用的转换器列表
 */
class CompositeTransformer(
    private val transformers: List<DocumentTransformer>
) : DocumentTransformer {
    
    constructor(vararg transformers: DocumentTransformer) : this(transformers.toList())
    
    override fun transform(document: Document): Document {
        var result = document
        for (transformer in transformers) {
            result = transformer.transform(result)
        }
        return result
    }
}
