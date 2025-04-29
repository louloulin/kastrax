package ai.kastrax.a2x.multimodal

import ai.kastrax.a2x.semantic.Context
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class MultimodalProcessorTest {
    
    private lateinit var processor: MultimodalProcessor
    
    @BeforeEach
    fun setUp() {
        processor = MultimodalProcessor()
        
        // 注册处理器
        processor.registerModalityProcessor(TextProcessor())
        processor.registerModalityProcessor(ImageProcessor())
        processor.registerModalityProcessor(AudioProcessor())
        processor.registerModalityProcessor(VideoProcessor())
        
        // 注册融合策略
        processor.registerFusionStrategy("simple", SimpleFusionStrategy())
        processor.registerFusionStrategy("weighted", WeightedFusionStrategy())
        
        // 设置默认融合策略
        processor.setFusionStrategy("simple")
    }
    
    @Test
    fun `test process multimodal input with text and image`() {
        // 创建多模态输入
        val input = MultimodalInput(
            modalityInputs = mapOf(
                "text" to JsonPrimitive("This is a test email: test@example.com and a URL: https://example.com"),
                "image" to JsonPrimitive("https://example.com/image.jpg")
            )
        )
        
        // 处理输入
        val result = processor.processMultimodalInput(input)
        
        // 验证结果
        assertNotNull(result)
        assertEquals(2, result.modalityResults.size)
        assertTrue(result.modalityResults.containsKey("text"))
        assertTrue(result.modalityResults.containsKey("image"))
        
        // 简化版本不再提取实体
        assertTrue(result.entities.isEmpty())
    }
    
    @Test
    fun `test analyze cross modal relationships`() {
        // 创建多模态输入
        val input = MultimodalInput(
            modalityInputs = mapOf(
                "text" to JsonPrimitive("This is a description of an image"),
                "image" to JsonPrimitive("https://example.com/image.jpg")
            )
        )
        
        // 分析跨模态关系
        val relationships = processor.analyzeCrossModalRelationships(input)
        
        // 验证关系
        assertNotNull(relationships)
        assertEquals(1, relationships.size)
        
        val relationship = relationships[0]
        assertTrue(
            (relationship.sourceModality == "text" && relationship.targetModality == "image") ||
            (relationship.sourceModality == "image" && relationship.targetModality == "text")
        )
        
        if (relationship.sourceModality == "text" && relationship.targetModality == "image") {
            assertEquals("describes", relationship.type)
        } else {
            assertEquals("illustrates", relationship.type)
        }
        
        assertTrue(relationship.strength > 0.0)
    }
    
    @Test
    fun `test weighted fusion strategy`() {
        // 设置加权融合策略
        processor.setFusionStrategy("weighted")
        
        // 创建多模态输入
        val input = MultimodalInput(
            modalityInputs = mapOf(
                "text" to JsonPrimitive("This is a test"),
                "image" to JsonPrimitive("https://example.com/image.jpg")
            )
        )
        
        // 处理输入
        val result = processor.processMultimodalInput(input)
        
        // 验证结果
        assertNotNull(result)
        assertEquals(2, result.modalityResults.size)
        
        // 验证融合结果包含权重信息
        val fusedResult = result.fusedResult
        assertTrue(fusedResult.containsKey("text"))
        assertTrue(fusedResult.containsKey("image"))
        
        // 验证包含权重信息
        val fusedResultString = fusedResult.toString()
        assertTrue(fusedResultString.contains("weight"))
    }
    
    @Test
    fun `test with context`() {
        // 创建上下文
        val context = Context(
            id = "ctx-${UUID.randomUUID()}",
            name = "Test Context",
            description = "A test context",
            type = "test",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            data = buildJsonObject {
                put("key", JsonPrimitive("value"))
            }
        )
        
        // 创建多模态输入
        val input = MultimodalInput(
            modalityInputs = mapOf(
                "text" to JsonPrimitive("This is a test"),
                "image" to JsonPrimitive("https://example.com/image.jpg")
            ),
            context = context
        )
        
        // 处理输入
        val result = processor.processMultimodalInput(input)
        
        // 验证结果
        assertNotNull(result)
        assertNotNull(result.context)
        assertEquals(context.id, result.context?.id)
        
        // 验证融合结果包含上下文信息
        val fusedResult = result.fusedResult
        assertTrue(fusedResult.containsKey("context"))
        
        // 验证上下文信息包含ID
        val fusedResultString = fusedResult.toString()
        assertTrue(fusedResultString.contains(context.id))
    }
}
