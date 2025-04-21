package ai.kastrax.rag.document.transform

import ai.kastrax.rag.document.Document
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.File
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

private val logger = KotlinLogging.logger {}

/**
 * 表格提取器，用于从 HTML、Excel、Word 和其他格式中提取表格数据。
 */
class TableExtractor {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    /**
     * 从文档中提取表格。
     *
     * @param document 包含表格的文档
     * @param options 提取选项
     * @return 提取的表格列表
     */
    fun extract(document: Document, options: ExtractionOptions = ExtractionOptions()): List<Table> {
        val source = document.metadata["source"] as? String
        val fileExtension = document.metadata["file_extension"] as? String

        return when {
            source != null && source.endsWith(".xlsx", ignoreCase = true) -> {
                extractFromExcel(File(source), options)
            }
            source != null && source.endsWith(".docx", ignoreCase = true) -> {
                extractFromWord(File(source), options)
            }
            fileExtension == "json" -> {
                extractFromJson(document.content, options)
            }
            else -> {
                // 默认尝试从 HTML 提取
                extractFromHtml(document.content, options)
            }
        }
    }

    /**
     * 从 HTML 字符串中提取表格。
     *
     * @param html HTML 字符串
     * @param options 提取选项
     * @return 提取的表格列表
     */
    fun extractFromHtml(html: String, options: ExtractionOptions = ExtractionOptions()): List<Table> {
        try {
            val doc = Jsoup.parse(html)
            val tables = doc.select("table")

            return tables.mapIndexed { index, table ->
                parseHtmlTable(table, index, options)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error extracting tables from HTML" }
            return emptyList()
        }
    }

    /**
     * 从 Excel 文件中提取表格。
     *
     * 注意：此方法需要添加 Apache POI 依赖才能正常工作。
     * 当前实现返回一个空列表，因为缺少依赖。
     *
     * @param file Excel 文件
     * @param options 提取选项
     * @return 提取的表格列表
     */
    fun extractFromExcel(file: File, options: ExtractionOptions = ExtractionOptions()): List<Table> {
        logger.warn { "Excel extraction is not supported in this version. Add Apache POI dependencies to enable this feature." }
        return emptyList()
    }

    /**
     * 从 Word 文件中提取表格。
     *
     * 注意：此方法需要添加 Apache POI 依赖才能正常工作。
     * 当前实现返回一个空列表，因为缺少依赖。
     *
     * @param file Word 文件
     * @param options 提取选项
     * @return 提取的表格列表
     */
    fun extractFromWord(file: File, options: ExtractionOptions = ExtractionOptions()): List<Table> {
        logger.warn { "Word extraction is not supported in this version. Add Apache POI dependencies to enable this feature." }
        return emptyList()
    }

    /**
     * 从 JSON 字符串中提取表格。
     *
     * @param json JSON 字符串
     * @param options 提取选项
     * @return 提取的表格列表
     */
    fun extractFromJson(json: String, options: ExtractionOptions = ExtractionOptions()): List<Table> {
        try {
            // 先尝试解析为对象数组（每个对象是一行）
            try {
                val rows = objectMapper.readValue<List<Map<String, Any>>>(json)
                if (rows.isNotEmpty()) {
                    // 确保所有行都有相同的键
                    val allKeys = rows.flatMap { it.keys }.distinct().sorted()
                    val tableRows = rows.map { row ->
                        allKeys.map { key -> row[key]?.toString() ?: "" }
                    }

                    return listOf(
                        Table(
                            id = "json-0",
                            name = "JSON Table",
                            headers = allKeys,
                            rows = tableRows,
                            source = "json",
                            metadata = mapOf("format" to "object_array")
                        )
                    )
                }
            } catch (e: Exception) {
                // 如果不是对象数组，继续尝试其他格式
                logger.debug(e) { "Not an object array, trying other formats" }
            }

            // 尝试解析为表格数组
            try {
                val tables = objectMapper.readValue<List<Map<String, Any>>>(json)
                return tables.mapIndexed { index, tableData ->
                    parseJsonTable(tableData, index)
                }
            } catch (e: Exception) {
                // 尝试解析为单个表格
                try {
                    val tableData = objectMapper.readValue<Map<String, Any>>(json)
                    return listOf(parseJsonTable(tableData, 0))
                } catch (e2: Exception) {
                    logger.error(e2) { "Error parsing JSON as table" }
                    return emptyList()
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error extracting tables from JSON" }
            return emptyList()
        }
    }

    /**
     * 解析 HTML 表格元素。
     *
     * @param table HTML 表格元素
     * @param index 表格索引
     * @param options 提取选项
     * @return 解析后的表格
     */
    private fun parseHtmlTable(table: Element, index: Int, options: ExtractionOptions): Table {
        // 提取表格标题
        val caption = table.select("caption").firstOrNull()?.text() ?: "Table ${index + 1}"

        // 提取表头
        val headerRow = table.select("thead tr").firstOrNull() ?: table.select("tr").firstOrNull()
        val headers = headerRow?.select("th, td")?.map { it.text() } ?: emptyList()

        // 提取数据行
        val rows = mutableListOf<List<String>>()
        val dataRows = if (table.select("thead").isNotEmpty()) {
            table.select("tbody tr")
        } else {
            // 如果没有 thead，跳过第一行（假设是表头）
            table.select("tr").drop(1)
        }

        for (row in dataRows) {
            val cells = row.select("td")
            if (cells.isNotEmpty()) {
                val rowData = cells.map { it.text() }
                rows.add(rowData)
            }
        }

        // 处理表格属性
        val tableAttributes = mutableMapOf<String, String>()
        for (attribute in table.attributes()) {
            tableAttributes[attribute.key] = attribute.value
        }

        return Table(
            id = "html-$index",
            name = caption,
            headers = headers,
            rows = rows,
            source = "html",
            metadata = mapOf(
                "attributes" to tableAttributes,
                "class" to (table.attr("class") ?: ""),
                "id" to (table.attr("id") ?: "")
            )
        )
    }

    // Word 表格解析方法已移除，因为缺少 Apache POI 依赖

    /**
     * 解析 JSON 表格数据。
     *
     * @param tableData JSON 表格数据
     * @param index 表格索引
     * @return 解析后的表格
     */
    private fun parseJsonTable(tableData: Map<String, Any>, index: Int): Table {
        val name = tableData["name"]?.toString() ?: "Table ${index + 1}"

        // 提取表头
        val headers = when (val headersData = tableData["headers"]) {
            is List<*> -> headersData.map { it?.toString() ?: "" }
            is Array<*> -> headersData.map { it?.toString() ?: "" }
            else -> emptyList()
        }

        // 提取数据行
        val rows = when (val rowsData = tableData["rows"] ?: tableData["data"]) {
            is List<*> -> {
                rowsData.map { row ->
                    when (row) {
                        is List<*> -> row.map { it?.toString() ?: "" }
                        is Array<*> -> row.map { it?.toString() ?: "" }
                        is Map<*, *> -> headers.map { header -> row[header]?.toString() ?: "" }
                        else -> emptyList()
                    }
                }
            }
            is Array<*> -> {
                rowsData.map { row ->
                    when (row) {
                        is List<*> -> row.map { it?.toString() ?: "" }
                        is Array<*> -> row.map { it?.toString() ?: "" }
                        is Map<*, *> -> headers.map { header -> row[header]?.toString() ?: "" }
                        else -> emptyList()
                    }
                }
            }
            else -> emptyList()
        }

        // 提取元数据
        val metadata = tableData.filter { it.key !in setOf("name", "headers", "rows", "data") }
            .mapValues { it.value.toString() }

        return Table(
            id = "json-$index",
            name = name,
            headers = headers,
            rows = rows,
            source = "json",
            metadata = metadata
        )
    }

    /**
     * 表格数据类，表示提取的表格。
     *
     * @property id 表格 ID
     * @property name 表格名称
     * @property headers 表头
     * @property rows 数据行
     * @property source 表格来源
     * @property metadata 表格元数据
     */
    data class Table(
        val id: String,
        val name: String,
        val headers: List<String>,
        val rows: List<List<String>>,
        val source: String,
        val metadata: Map<String, Any> = emptyMap()
    ) {
        /**
         * 将表格转换为 CSV 格式。
         *
         * @param delimiter 分隔符，默认为逗号
         * @param includeHeaders 是否包含表头，默认为 true
         * @return CSV 格式的字符串
         */
        fun toCsv(delimiter: String = ",", includeHeaders: Boolean = true): String {
            val sb = StringBuilder()

            // 添加表头
            if (includeHeaders && headers.isNotEmpty()) {
                sb.append(headers.joinToString(delimiter) { escapeForCsv(it, delimiter) })
                sb.append("\n")
            }

            // 添加数据行
            for (row in rows) {
                sb.append(row.joinToString(delimiter) { escapeForCsv(it, delimiter) })
                sb.append("\n")
            }

            return sb.toString()
        }

        /**
         * 将表格转换为 Markdown 格式。
         *
         * @return Markdown 格式的字符串
         */
        fun toMarkdown(): String {
            val sb = StringBuilder()

            // 添加表格标题
            sb.append("### $name\n\n")

            // 如果没有数据，返回空表格提示
            if (headers.isEmpty() && rows.isEmpty()) {
                sb.append("*Empty table*\n\n")
                return sb.toString()
            }

            // 确定列数
            val columnCount = if (headers.isNotEmpty()) {
                headers.size
            } else if (rows.isNotEmpty()) {
                rows.first().size
            } else {
                0
            }

            if (columnCount == 0) {
                sb.append("*Empty table*\n\n")
                return sb.toString()
            }

            // 添加表头
            if (headers.isNotEmpty()) {
                sb.append("| ")
                sb.append(headers.joinToString(" | "))
                sb.append(" |\n")
            } else {
                sb.append("| ")
                sb.append(List(columnCount) { "Column ${it + 1}" }.joinToString(" | "))
                sb.append(" |\n")
            }

            // 添加分隔行
            sb.append("| ")
            sb.append(List(columnCount) { "---" }.joinToString(" | "))
            sb.append(" |\n")

            // 添加数据行
            for (row in rows) {
                sb.append("| ")
                // 确保行中的单元格数量与表头一致
                val paddedRow = if (row.size < columnCount) {
                    row + List(columnCount - row.size) { "" }
                } else {
                    row.take(columnCount)
                }
                sb.append(paddedRow.joinToString(" | "))
                sb.append(" |\n")
            }

            sb.append("\n")
            return sb.toString()
        }

        /**
         * 将表格转换为 JSON 格式。
         *
         * @param pretty 是否美化输出，默认为 false
         * @return JSON 格式的字符串
         */
        fun toJson(pretty: Boolean = false): String {
            val objectMapper = jacksonObjectMapper()

            val tableData = mapOf(
                "id" to id,
                "name" to name,
                "headers" to headers,
                "rows" to rows,
                "source" to source,
                "metadata" to metadata
            )

            return if (pretty) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tableData)
            } else {
                objectMapper.writeValueAsString(tableData)
            }
        }

        /**
         * 将表格转换为 HTML 格式。
         *
         * @param includeStyles 是否包含样式，默认为 true
         * @return HTML 格式的字符串
         */
        fun toHtml(includeStyles: Boolean = true): String {
            val sb = StringBuilder()

            // 添加样式
            if (includeStyles) {
                sb.append("""
                    <style>
                    table {
                        border-collapse: collapse;
                        width: 100%;
                        margin-bottom: 1em;
                    }
                    th, td {
                        border: 1px solid #ddd;
                        padding: 8px;
                        text-align: left;
                    }
                    th {
                        background-color: #f2f2f2;
                        font-weight: bold;
                    }
                    tr:nth-child(even) {
                        background-color: #f9f9f9;
                    }
                    caption {
                        font-weight: bold;
                        margin-bottom: 0.5em;
                    }
                    </style>
                """.trimIndent())
                sb.append("\n")
            }

            // 开始表格
            sb.append("<table>\n")

            // 添加标题
            sb.append("  <caption>$name</caption>\n")

            // 添加表头
            if (headers.isNotEmpty()) {
                sb.append("  <thead>\n    <tr>\n")
                for (header in headers) {
                    sb.append("      <th>$header</th>\n")
                }
                sb.append("    </tr>\n  </thead>\n")
            }

            // 添加数据行
            sb.append("  <tbody>\n")
            for (row in rows) {
                sb.append("    <tr>\n")
                for (cell in row) {
                    sb.append("      <td>$cell</td>\n")
                }
                sb.append("    </tr>\n")
            }
            sb.append("  </tbody>\n")

            // 结束表格
            sb.append("</table>\n")

            return sb.toString()
        }

        /**
         * 将表格转换为文档。
         *
         * @param format 输出格式，可选值为 "csv"、"markdown"、"json" 或 "html"
         * @return 包含表格数据的文档
         */
        fun toDocument(format: String = "markdown"): Document {
            val content = when (format.lowercase()) {
                "csv" -> toCsv()
                "markdown" -> toMarkdown()
                "json" -> toJson(true)
                "html" -> toHtml()
                else -> toMarkdown()
            }

            val metadata = mutableMapOf<String, Any>(
                "table_id" to id,
                "table_name" to name,
                "table_source" to source,
                "table_format" to format,
                "table_headers" to headers,
                "table_row_count" to rows.size
            )

            // 添加原始元数据
            for ((key, value) in this.metadata) {
                metadata["table_metadata_$key"] = value
            }

            return Document(content, metadata)
        }

        /**
         * 转义 CSV 中的特殊字符。
         *
         * @param value 要转义的值
         * @param delimiter 分隔符
         * @return 转义后的值
         */
        private fun escapeForCsv(value: String, delimiter: String): String {
            return if (value.contains(delimiter) || value.contains("\"") || value.contains("\n")) {
                "\"" + value.replace("\"", "\"\"") + "\""
            } else {
                value
            }
        }
    }

    /**
     * 表格提取选项。
     *
     * @property includeEmptyTables 是否包含空表格，默认为 false
     * @property minRows 最小行数，默认为 1
     * @property minColumns 最小列数，默认为 1
     * @property extractCaptions 是否提取表格标题，默认为 true
     * @property normalizeHeaders 是否规范化表头，默认为 true
     */
    data class ExtractionOptions(
        val includeEmptyTables: Boolean = false,
        val minRows: Int = 1,
        val minColumns: Int = 1,
        val extractCaptions: Boolean = true,
        val normalizeHeaders: Boolean = true
    ) {
        /**
         * 创建一个新的 ExtractionOptions 构建器。
         *
         * @return ExtractionOptionsBuilder 实例
         */
        fun toBuilder(): ExtractionOptionsBuilder {
            return ExtractionOptionsBuilder(this)
        }

        /**
         * ExtractionOptions 构建器，用于链式配置提取选项。
         */
        class ExtractionOptionsBuilder(options: ExtractionOptions) {
            private var includeEmptyTables = options.includeEmptyTables
            private var minRows = options.minRows
            private var minColumns = options.minColumns
            private var extractCaptions = options.extractCaptions
            private var normalizeHeaders = options.normalizeHeaders

            fun includeEmptyTables(value: Boolean) = apply { includeEmptyTables = value }
            fun minRows(value: Int) = apply { minRows = value }
            fun minColumns(value: Int) = apply { minColumns = value }
            fun extractCaptions(value: Boolean) = apply { extractCaptions = value }
            fun normalizeHeaders(value: Boolean) = apply { normalizeHeaders = value }

            fun build(): ExtractionOptions {
                return ExtractionOptions(
                    includeEmptyTables = includeEmptyTables,
                    minRows = minRows,
                    minColumns = minColumns,
                    extractCaptions = extractCaptions,
                    normalizeHeaders = normalizeHeaders
                )
            }
        }
    }
}
