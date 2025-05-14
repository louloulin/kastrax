package ai.kastrax.codebase.semantic.pattern.detector

import ai.kastrax.codebase.semantic.flow.FlowGraph
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.pattern.PatternCategory
import ai.kastrax.codebase.semantic.pattern.PatternMatch
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 设计模式检测器
 *
 * 检测代码中的设计模式
 */
class DesignPatternDetector : PatternDetector {
    private val logger = KotlinLogging.logger {}

    /**
     * 从代码元素中检测模式
     */
    override suspend fun detectPatterns(element: CodeElement): List<PatternMatch> {
        logger.debug { "从代码元素中检测设计模式" }
        // 暂时返回空列表，等待实现
        return emptyList()
    }

    /**
     * 获取检测器支持的模式类别
     */
    override fun getCategory(): PatternCategory {
        return PatternCategory.DESIGN_PATTERN
    }

    /**
     * 获取检测器支持的模式列表
     */
    override fun getSupportedPatterns(): List<Pair<String, String>> {
        return listOf(
            "singleton" to "单例模式",
            "factory" to "工厂模式",
            "builder" to "建造者模式",
            "adapter" to "适配器模式",
            "chain_of_responsibility" to "责任链模式"
        )
    }
}
