package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.tools.Tool
import ai.kastrax.memory.api.UpdateWorkingMemoryParams
import ai.kastrax.memory.api.UpdateWorkingMemoryResult
import ai.kastrax.memory.api.WorkingMemory
import ai.kastrax.memory.api.WorkingMemoryConfig
import ai.kastrax.memory.api.WorkingMemoryMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import javax.sql.DataSource

/**
 * PostgreSQL实现的工作内存。
 *
 * @property dataSource 数据源
 * @property tableName 表名
 */
class PostgresWorkingMemory(
    private val dataSource: DataSource,
    private val tableName: String = "working_memory"
) : WorkingMemory, KastraXBase(component = "WORKING_MEMORY", name = "postgres") {

    init {
        // 初始化表结构
        initTable()
    }

    /**
     * 初始化表结构。
     */
    private fun initTable() {
        try {
            dataSource.connection.use { connection ->
                val createTableSql = """
                    CREATE TABLE IF NOT EXISTS $tableName (
                        thread_id VARCHAR(255) PRIMARY KEY,
                        content TEXT NOT NULL,
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
                    )
                """.trimIndent()

                connection.createStatement().use { statement ->
                    statement.execute(createTableSql)
                }

                // 创建更新触发器，自动更新updated_at字段
                val triggerSql = """
                    CREATE OR REPLACE FUNCTION update_updated_at_column()
                    RETURNS TRIGGER AS $$
                    BEGIN
                        NEW.updated_at = CURRENT_TIMESTAMP;
                        RETURN NEW;
                    END;
                    $$ LANGUAGE plpgsql;

                    DROP TRIGGER IF EXISTS update_${tableName}_updated_at ON $tableName;

                    CREATE TRIGGER update_${tableName}_updated_at
                    BEFORE UPDATE ON $tableName
                    FOR EACH ROW
                    EXECUTE FUNCTION update_updated_at_column();
                """.trimIndent()

                connection.createStatement().use { statement ->
                    statement.execute(triggerSql)
                }
            }
        } catch (e: SQLException) {
            logger.error("初始化PostgreSQL工作内存表失败: ${e.message}")
            throw IllegalStateException("初始化PostgreSQL工作内存表失败", e)
        }
    }

    override suspend fun getWorkingMemory(threadId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { connection ->
                    val sql = "SELECT content FROM $tableName WHERE thread_id = ?"
                    connection.prepareStatement(sql).use { statement ->
                        statement.setString(1, threadId)
                        statement.executeQuery().use { resultSet ->
                            if (resultSet.next()) {
                                resultSet.getString("content")
                            } else {
                                null
                            }
                        }
                    }
                }
            } catch (e: SQLException) {
                logger.error("从PostgreSQL获取工作内存失败: ${e.message}")
                null
            }
        }
    }

    override suspend fun updateWorkingMemory(threadId: String, content: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { connection ->
                    // 使用UPSERT语法（INSERT ... ON CONFLICT DO UPDATE）
                    val sql = """
                        INSERT INTO $tableName (thread_id, content)
                        VALUES (?, ?)
                        ON CONFLICT (thread_id)
                        DO UPDATE SET content = EXCLUDED.content
                    """.trimIndent()

                    connection.prepareStatement(sql).use { statement ->
                        statement.setString(1, threadId)
                        statement.setString(2, content)
                        statement.executeUpdate()
                    }
                    true
                }
            } catch (e: SQLException) {
                logger.error("更新PostgreSQL工作内存失败: ${e.message}")
                false
            }
        }
    }

    override suspend fun getSystemMessage(threadId: String, config: WorkingMemoryConfig?): String? {
        val cfg = config ?: WorkingMemoryConfig()
        if (!cfg.enabled) return null

        val memory = getWorkingMemory(threadId) ?: cfg.template

        return when (cfg.mode) {
            WorkingMemoryMode.TEXT_STREAM -> {
                """
                # 工作内存
                以下是你的工作内存，包含关于用户和对话的重要信息。请在回答时参考这些信息。

                $memory
                """.trimIndent()
            }
            WorkingMemoryMode.TOOL_CALL -> {
                """
                # 工作内存
                你可以使用update_working_memory工具来更新工作内存。当前工作内存内容：

                $memory
                """.trimIndent()
            }
        }
    }

    override fun getTools(config: WorkingMemoryConfig?): Map<String, Tool> {
        val cfg = config ?: WorkingMemoryConfig()
        if (!cfg.enabled || cfg.mode != WorkingMemoryMode.TOOL_CALL) {
            return emptyMap()
        }

        val updateWorkingMemoryTool = ai.kastrax.core.tools.tool {
            id = "update_working_memory"
            name = "更新工作内存"
            description = "更新工作内存中的信息"
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("memory") {
                        put("type", "string")
                        put("description", "新的工作内存内容")
                    }
                }
                put("required", JsonArray(listOf(JsonPrimitive("memory"))))
            }
            execute = { input ->
                // 解析输入
                val inputStr = input.toString()
                val threadId = inputStr.substringAfter("threadId=").substringBefore(",")

                // 检查线程ID
                if (threadId.isBlank()) {
                    buildJsonObject {
                        put("success", false)
                        put("error", "未提供线程ID")
                    }
                }

                // 解析内存内容
                val memoryContent = try {
                    // 从输入中提取memory参数
                    val memoryParam = if (inputStr.contains("memory=")) {
                        inputStr.substringAfter("memory=").substringBefore(",")
                    } else {
                        inputStr // 假设整个输入就是内存内容
                    }

                    memoryParam
                } catch (e: Exception) {
                    logger.error("解析工作内存参数失败: ${e.message}")
                    buildJsonObject {
                        put("success", false)
                        put("error", "解析工作内存参数失败: ${e.message}")
                    }
                }

                // 更新工作内存
                try {
                    val success = updateWorkingMemory(threadId, memoryContent.toString())

                    buildJsonObject {
                        put("success", success)
                    }
                } catch (e: Exception) {
                    logger.error("更新工作内存失败: ${e.message}")
                    buildJsonObject {
                        put("success", false)
                        put("error", "更新工作内存失败: ${e.message}")
                    }
                }
            }
        }

        return mapOf("update_working_memory" to updateWorkingMemoryTool)
    }

    /**
     * 清理过期的工作内存。
     *
     * @param days 过期天数
     * @return 清理的记录数
     */
    suspend fun cleanupExpiredMemories(days: Int): Int {
        return withContext(Dispatchers.IO) {
            try {
                dataSource.connection.use { connection ->
                    val sql = "DELETE FROM $tableName WHERE updated_at < NOW() - INTERVAL '$days days'"
                    connection.prepareStatement(sql).use { statement ->
                        statement.executeUpdate()
                    }
                }
            } catch (e: SQLException) {
                logger.error("清理过期工作内存失败: ${e.message}")
                0
            }
        }
    }
}
