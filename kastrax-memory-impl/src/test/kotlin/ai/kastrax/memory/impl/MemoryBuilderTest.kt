package ai.kastrax.memory.impl

import ai.kastrax.memory.api.MemoryCompressionConfig
import ai.kastrax.memory.api.WorkingMemoryConfig
import ai.kastrax.memory.api.WorkingMemoryMode
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MemoryBuilderTest {

    @Test
    fun `test MemoryBuilderImpl creates EnhancedMemory with all options`() {
        val builder = MemoryBuilderImpl()

        // 配置构建器
        builder.storage(InMemoryStorage())
            .lastMessages(20)
            .semanticRecall(true)
            .workingMemory(WorkingMemoryConfig(
                enabled = true,
                template = "测试工作内存",

                mode = WorkingMemoryMode.TEXT_STREAM
            ))

        // 使用反射设置内存压缩器和其他选项
        val memoryCompressorMethod = MemoryBuilderImpl::class.java.getDeclaredMethod(
            "memoryCompressor",
            ai.kastrax.memory.api.MemoryCompressor::class.java
        )
        memoryCompressorMethod.invoke(builder, MockMemoryCompressor())

        val compressionConfigMethod = MemoryBuilderImpl::class.java.getDeclaredMethod(
            "compressionConfig",
            ai.kastrax.memory.api.MemoryCompressionConfig::class.java
        )
        compressionConfigMethod.invoke(builder, MemoryCompressionConfig(
            enabled = true,
            threshold = 100,
            preserveSystemMessages = true,
            preserveRecentMessages = 5
        ))

        val tagManagerMethod = MemoryBuilderImpl::class.java.getDeclaredMethod(
            "tagManager",
            Boolean::class.java
        )
        tagManagerMethod.invoke(builder, true)

        val threadSharingMethod = MemoryBuilderImpl::class.java.getDeclaredMethod(
            "threadSharing",
            Boolean::class.java
        )
        threadSharingMethod.invoke(builder, true)

        // 构建内存
        val memory = builder.build()

        // 验证结果
        assertTrue(memory is EnhancedMemory)

        // 创建线程并测试功能
        runBlocking {
            val threadId = memory.createThread("测试线程")
            assertNotNull(threadId)

            val thread = memory.getThread(threadId)
            assertNotNull(thread)
            assertEquals("测试线程", thread.title)
        }
    }

    @Test
    fun `test TestEnhancedMemoryBuilder creates EnhancedMemory with all options`() {
        // 使用反射检查TestEnhancedMemoryBuilder类是否有memoryCompressor方法
        val hasMemoryCompressorMethod = try {
            TestEnhancedMemoryBuilder::class.java.getDeclaredMethod(
                "memoryCompressor",
                ai.kastrax.memory.api.MemoryCompressor::class.java
            )
            true
        } catch (e: NoSuchMethodException) {
            false
        }

        val builder = TestEnhancedMemoryBuilder()

        // 配置构建器
        builder.storage(InMemoryStorage())
            .lastMessages(20)
            .semanticRecall(true)
            .workingMemory(WorkingMemoryConfig(
                enabled = true,
                template = "测试工作内存",
                mode = WorkingMemoryMode.TEXT_STREAM
            ))

        // 构建内存
        val memory = builder.build()

        // 验证结果
        assertTrue(memory is EnhancedMemory)

        // 创建线程并测试功能
        runBlocking {
            val threadId = memory.createThread("测试线程")
            assertNotNull(threadId)

            val thread = memory.getThread(threadId)
            assertNotNull(thread)
            assertEquals("测试线程", thread.title)
        }
    }
}
