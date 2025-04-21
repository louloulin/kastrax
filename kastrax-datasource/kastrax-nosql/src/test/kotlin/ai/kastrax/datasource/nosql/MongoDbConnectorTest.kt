package ai.kastrax.datasource.nosql

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

/**
 * MongoDB 连接器测试类。
 * 
 * 注意：这些测试需要 MongoDB 容器，目前已禁用。
 */
@Disabled("Requires MongoDB container")
class MongoDbConnectorTest {
    
    @Test
    fun `test simple assertion`() {
        // Simple test that doesn't require MongoDB
        assertTrue(true)
    }
}
