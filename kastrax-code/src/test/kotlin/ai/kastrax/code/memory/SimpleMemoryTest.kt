package ai.kastrax.code.memory

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * 简单记忆测试类
 */
class SimpleMemoryTest {

    @Test
    fun testSimpleMemoryCreation() {
        val memory = SimpleMemory(
            content = "Test content",
            metadata = mapOf("key" to "value"),
            timestamp = Instant.now()
        )

        assertEquals("Test content", memory.content)
        assertEquals("value", memory.metadata["key"])
    }
}
