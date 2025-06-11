package ai.kastrax.edutech

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * 简单的测试运行器，用于运行Week 21-22多模态智能教学助手测试
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestRunner {
    
    @Test
    fun runMultimodalTeachingTests() = runBlocking {
        println("🚀 开始运行Week 21-22多模态智能教学助手测试...")
        
        val testInstance = Week21_22MultimodalTeachingTest()
        testInstance.setup()
        
        try {
            // 运行所有测试
            println("\n🎤 运行语音交互教学助手测试...")
            testInstance.testVoiceInteractionTeachingAssistant()
            
            println("\n👁️ 运行视觉内容理解和生成测试...")
            testInstance.testVisualContentProcessing()
            
            println("\n🎨 运行多模态学习内容创建测试...")
            testInstance.testMultimodalContentCreation()
            
            println("\n🤖 运行智能问答和解释系统测试...")
            testInstance.testIntelligentQASystem()
            
            println("\n📚 运行个性化教学策略推荐测试...")
            testInstance.testPersonalizedTeachingStrategies()
            
            println("\n🔗 运行多模态教学服务集成测试...")
            testInstance.testMultimodalTeachingServiceIntegration()
            
            testInstance.cleanup()
            
            println("\n✅ 所有Week 21-22多模态智能教学助手测试通过！")
            
        } catch (e: Exception) {
            println("\n❌ 测试失败: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
}
