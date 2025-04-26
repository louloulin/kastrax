package ai.kastrax.core.agent

import ai.kastrax.core.common.KastraXBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement

/**
 * SQLite实现的状态管理器
 *
 * @property dbPath SQLite数据库文件路径
 * @property json JSON序列化器
 */
class SQLiteStateManager(
    private val dbPath: String,
    private val json: Json = Json { ignoreUnknownKeys = true; prettyPrint = false }
) : StateManager, KastraXBase(component = "STATE_MANAGER", name = "sqlite") {

    init {
        // 初始化数据库
        initDatabase()
    }

    /**
     * 初始化数据库
     */
    private fun initDatabase() {
        var connection: Connection? = null
        var statement: Statement? = null

        try {
            // 加载SQLite JDBC驱动
            Class.forName("org.sqlite.JDBC")

            // 创建连接
            connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
            statement = connection.createStatement()

            // 创建状态表
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS agent_states (
                    id TEXT PRIMARY KEY,
                    thread_id TEXT,
                    resource_id TEXT,
                    status TEXT NOT NULL,
                    metadata TEXT NOT NULL,
                    variables TEXT NOT NULL,
                    last_updated TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
            """)

            // 创建索引
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_thread_id ON agent_states(thread_id)")
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_resource_id ON agent_states(resource_id)")

            logger.info { "SQLite状态管理器初始化完成: $dbPath" }
        } catch (e: Exception) {
            logger.error(e) { "初始化SQLite数据库失败: ${e.message}" }
            throw e
        } finally {
            statement?.close()
            connection?.close()
        }
    }

    /**
     * 获取数据库连接
     */
    private fun getConnection(): Connection {
        return DriverManager.getConnection("jdbc:sqlite:$dbPath")
    }

    /**
     * 从ResultSet中解析AgentState
     */
    private fun parseAgentState(rs: ResultSet): AgentState {
        val id = rs.getString("id")
        val threadId = rs.getString("thread_id")
        val resourceId = rs.getString("resource_id")
        val status = AgentStatus.valueOf(rs.getString("status"))
        val metadata = json.decodeFromString<Map<String, String>>(rs.getString("metadata"))
        val variables = json.decodeFromString<Map<String, JsonElement>>(rs.getString("variables"))
        val lastUpdated = Instant.parse(rs.getString("last_updated"))
        val createdAt = Instant.parse(rs.getString("created_at"))

        return AgentState(
            id = id,
            threadId = threadId,
            resourceId = resourceId,
            status = status,
            metadata = metadata,
            variables = variables,
            lastUpdated = lastUpdated,
            createdAt = createdAt
        )
    }

    override suspend fun saveState(state: AgentState): AgentState = withContext(Dispatchers.IO) {
        val updatedState = state.copy(lastUpdated = Clock.System.now())
        var connection: Connection? = null

        try {
            connection = getConnection()
            connection.prepareStatement("""
                INSERT OR REPLACE INTO agent_states (
                    id, thread_id, resource_id, status, metadata, variables, last_updated, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """).use { stmt ->
                stmt.setString(1, updatedState.id)
                stmt.setString(2, updatedState.threadId)
                stmt.setString(3, updatedState.resourceId)
                stmt.setString(4, updatedState.status.name)
                stmt.setString(5, json.encodeToString(updatedState.metadata))
                stmt.setString(6, json.encodeToString(updatedState.variables))
                stmt.setString(7, updatedState.lastUpdated.toString())
                stmt.setString(8, updatedState.createdAt.toString())
                stmt.executeUpdate()
            }

            logger.debug { "保存状态: ${updatedState.id}" }
            return@withContext updatedState
        } catch (e: Exception) {
            logger.error(e) { "保存状态失败: ${e.message}" }
            throw e
        } finally {
            connection?.close()
        }
    }

    override suspend fun getState(stateId: String): AgentState? = withContext(Dispatchers.IO) {
        var connection: Connection? = null

        try {
            connection = getConnection()
            connection.prepareStatement("SELECT * FROM agent_states WHERE id = ?").use { stmt ->
                stmt.setString(1, stateId)
                val rs = stmt.executeQuery()

                if (rs.next()) {
                    return@withContext parseAgentState(rs)
                }
                return@withContext null
            }
        } catch (e: Exception) {
            logger.error(e) { "获取状态失败: ${e.message}" }
            throw e
        } finally {
            connection?.close()
        }
    }

    override suspend fun getStateByThread(threadId: String): AgentState? = withContext(Dispatchers.IO) {
        var connection: Connection? = null

        try {
            connection = getConnection()
            connection.prepareStatement("SELECT * FROM agent_states WHERE thread_id = ? ORDER BY last_updated DESC LIMIT 1").use { stmt ->
                stmt.setString(1, threadId)
                val rs = stmt.executeQuery()

                if (rs.next()) {
                    return@withContext parseAgentState(rs)
                }
                return@withContext null
            }
        } catch (e: Exception) {
            logger.error(e) { "获取线程状态失败: ${e.message}" }
            throw e
        } finally {
            connection?.close()
        }
    }

    override suspend fun getStatesByResource(resourceId: String): List<AgentState> = withContext(Dispatchers.IO) {
        var connection: Connection? = null
        val states = mutableListOf<AgentState>()

        try {
            connection = getConnection()
            connection.prepareStatement("SELECT * FROM agent_states WHERE resource_id = ? ORDER BY last_updated DESC").use { stmt ->
                stmt.setString(1, resourceId)
                val rs = stmt.executeQuery()

                while (rs.next()) {
                    states.add(parseAgentState(rs))
                }
                return@withContext states
            }
        } catch (e: Exception) {
            logger.error(e) { "获取资源状态失败: ${e.message}" }
            throw e
        } finally {
            connection?.close()
        }
    }

    override suspend fun deleteState(stateId: String): Boolean = withContext(Dispatchers.IO) {
        var connection: Connection? = null

        try {
            connection = getConnection()
            connection.prepareStatement("DELETE FROM agent_states WHERE id = ?").use { stmt ->
                stmt.setString(1, stateId)
                val count = stmt.executeUpdate()
                
                logger.debug { "删除状态: $stateId, 影响行数: $count" }
                return@withContext count > 0
            }
        } catch (e: Exception) {
            logger.error(e) { "删除状态失败: ${e.message}" }
            throw e
        } finally {
            connection?.close()
        }
    }
}
