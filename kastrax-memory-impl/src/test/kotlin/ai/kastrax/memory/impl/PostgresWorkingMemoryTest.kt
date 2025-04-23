package ai.kastrax.memory.impl

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * PostgreSQL工作内存测试。
 *
 * 注意：这个测试需要PostgreSQL容器，所以默认是禁用的。
 * 要启用这个测试，需要设置系统属性：-Dtest.postgres=true
 */
@EnabledIfSystemProperty(named = "test.postgres", matches = "true")
class PostgresWorkingMemoryTest {

    /**
     * 简单数据源实现，用于测试。
     */
    private class SimpleDataSource(private val url: String) : DataSource {
        override fun getConnection(): Connection = DriverManager.getConnection(url)

        override fun getConnection(username: String?, password: String?): Connection =
            DriverManager.getConnection(url, username, password)

        override fun getLogWriter() = null
        override fun setLogWriter(out: java.io.PrintWriter?) {}
        override fun setLoginTimeout(seconds: Int) {}
        override fun getLoginTimeout() = 0
        override fun getParentLogger() = java.util.logging.Logger.getGlobal()
        override fun isWrapperFor(iface: Class<*>) = false
        override fun <T : Any> unwrap(iface: Class<T>): T {
            throw SQLException("Unwrap not supported")
        }
    }

    @Test
    fun `test postgres working memory`() = runTest {
        // 创建数据源
        // 注意：这里使用的是本地PostgreSQL，实际测试中应该使用测试容器
        val dataSource = SimpleDataSource("jdbc:postgresql://localhost:5432/kastrax_test")

        // 创建PostgreSQL工作内存
        val workingMemory = PostgresWorkingMemory(dataSource)

        // 创建线程
        val threadId = "test-thread-${System.currentTimeMillis()}"

        // 更新工作内存（初始化）
        val updated = workingMemory.updateWorkingMemory(threadId, """{"memory": "Initial memory"}""")

        // 验证更新结果
        assertEquals(true, updated)

        // 获取工作内存
        val memory = workingMemory.getWorkingMemory(threadId)

        // 验证结果
        assertNotNull(memory)
        assertTrue(memory!!.contains("Initial memory"))

        // 更新工作内存
        val updatedAgain = workingMemory.updateWorkingMemory(threadId, """{"memory": "Updated memory"}""")

        // 验证更新结果
        assertEquals(true, updatedAgain)

        // 再次获取工作内存
        val updatedMemory = workingMemory.getWorkingMemory(threadId)

        // 验证更新后的结果
        assertTrue(updatedMemory!!.contains("Updated memory"))
    }
}
