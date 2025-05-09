package ai.kastrax.rag.document

import io.github.oshai.kotlinlogging.KotlinLogging
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.sql.Statement

private val logger = KotlinLogging.logger {}

/**
 * 数据库文档加载器，从数据库查询结果加载文档。
 *
 * @property url 数据库连接URL
 * @property username 数据库用户名
 * @property password 数据库密码
 * @property query SQL查询语句
 * @property metadata 要添加到文档的元数据
 * @property documentPerRow 是否为每行创建一个文档，默认为true
 * @property columnDelimiter 列之间的分隔符，用于构建文本内容，默认为": "
 * @property rowDelimiter 行之间的分隔符，用于构建文本内容，默认为"\n"
 * @property includeColumnNames 是否在内容中包含列名，默认为true
 * @property maxRows 最大行数，默认为1000
 */
class DatabaseDocumentLoader(
    private val url: String,
    private val username: String,
    private val password: String,
    private val query: String,
    private val metadata: Map<String, Any> = emptyMap(),
    private val documentPerRow: Boolean = true,
    private val columnDelimiter: String = ": ",
    private val rowDelimiter: String = "\n",
    private val includeColumnNames: Boolean = true,
    private val maxRows: Int = 1000
) : DocumentLoader {

    override suspend fun load(): List<Document> {
        logger.debug { "Loading documents from database query: $query" }

        var connection: Connection? = null
        var statement: Statement? = null
        var resultSet: ResultSet? = null

        return try {
            // 建立数据库连接
            connection = DriverManager.getConnection(url, username, password)
            statement = connection.createStatement()
            statement.maxRows = maxRows
            
            // 执行查询
            resultSet = statement.executeQuery(query)
            val metaData = resultSet.metaData
            val columnCount = metaData.columnCount
            
            // 获取列名
            val columnNames = (1..columnCount).map { metaData.getColumnLabel(it) }
            
            // 基础元数据
            val baseMetadata = mutableMapOf(
                "source" to "database",
                "database_url" to url,
                "query" to query,
                "column_count" to columnCount,
                "column_names" to columnNames
            )
            
            // 合并用户提供的元数据
            val fullMetadata = baseMetadata + metadata
            
            if (documentPerRow) {
                // 为每行创建一个文档
                val documents = mutableListOf<Document>()
                var rowIndex = 0
                
                while (resultSet.next() && rowIndex < maxRows) {
                    val rowContent = buildRowContent(resultSet, metaData, columnCount)
                    
                    val rowMetadata = fullMetadata + mapOf(
                        "row_index" to rowIndex,
                        "row_number" to (rowIndex + 1)
                    )
                    
                    documents.add(Document(rowContent, rowMetadata))
                    rowIndex++
                }
                
                documents
            } else {
                // 整个结果集作为一个文档
                val content = buildResultSetContent(resultSet, metaData, columnCount)
                
                listOf(Document(content, fullMetadata))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error loading documents from database: $url, query: $query" }
            emptyList()
        } finally {
            // 关闭资源
            try {
                resultSet?.close()
                statement?.close()
                connection?.close()
            } catch (e: Exception) {
                logger.error(e) { "Error closing database resources" }
            }
        }
    }

    /**
     * 为单行构建内容。
     */
    private fun buildRowContent(
        resultSet: ResultSet,
        metaData: ResultSetMetaData,
        columnCount: Int
    ): String {
        val builder = StringBuilder()
        
        for (i in 1..columnCount) {
            if (includeColumnNames) {
                builder.append(metaData.getColumnLabel(i))
                builder.append(columnDelimiter)
            }
            
            val value = resultSet.getString(i) ?: "NULL"
            builder.append(value)
            
            if (i < columnCount) {
                builder.append(rowDelimiter)
            }
        }
        
        return builder.toString()
    }

    /**
     * 为整个结果集构建内容。
     */
    private fun buildResultSetContent(
        resultSet: ResultSet,
        metaData: ResultSetMetaData,
        columnCount: Int
    ): String {
        val builder = StringBuilder()
        
        // 添加列名作为标题
        if (includeColumnNames) {
            for (i in 1..columnCount) {
                builder.append(metaData.getColumnLabel(i))
                if (i < columnCount) {
                    builder.append("\t")
                }
            }
            builder.append(rowDelimiter)
        }
        
        // 添加所有行
        var rowCount = 0
        resultSet.beforeFirst()  // 重置结果集指针
        
        while (resultSet.next() && rowCount < maxRows) {
            for (i in 1..columnCount) {
                val value = resultSet.getString(i) ?: "NULL"
                builder.append(value)
                if (i < columnCount) {
                    builder.append("\t")
                }
            }
            
            builder.append(rowDelimiter)
            rowCount++
        }
        
        return builder.toString()
    }
}
