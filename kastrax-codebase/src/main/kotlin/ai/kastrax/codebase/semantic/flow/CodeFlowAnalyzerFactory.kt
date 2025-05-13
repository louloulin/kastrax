package ai.kastrax.codebase.semantic.flow

import ai.kastrax.codebase.semantic.flow.impl.ControlFlowAnalyzerImpl
import ai.kastrax.codebase.semantic.flow.impl.DataFlowAnalyzerImpl
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

/**
 * 代码流分析器工厂
 *
 * 创建和管理代码流分析器
 */
object CodeFlowAnalyzerFactory {
    private val logger = KotlinLogging.logger {}
    
    // 流类型到分析器的映射
    private val analyzers = ConcurrentHashMap<FlowType, CodeFlowAnalyzer>()
    
    // 自定义分析器
    private val customAnalyzers = ConcurrentHashMap<String, CodeFlowAnalyzer>()
    
    /**
     * 初始化工厂
     *
     * @param config 配置
     */
    fun init(config: CodeFlowAnalyzerConfig = CodeFlowAnalyzerConfig()) {
        // 注册默认分析器
        registerAnalyzer(FlowType.CONTROL_FLOW, ControlFlowAnalyzerImpl(config))
        registerAnalyzer(FlowType.DATA_FLOW, DataFlowAnalyzerImpl(config))
        
        logger.info { "代码流分析器工厂初始化完成" }
    }
    
    /**
     * 注册分析器
     *
     * @param type 流类型
     * @param analyzer 分析器
     */
    fun registerAnalyzer(type: FlowType, analyzer: CodeFlowAnalyzer) {
        analyzers[type] = analyzer
        logger.info { "注册代码流分析器: $type" }
    }
    
    /**
     * 注册自定义分析器
     *
     * @param name 分析器名称
     * @param analyzer 分析器
     */
    fun registerCustomAnalyzer(name: String, analyzer: CodeFlowAnalyzer) {
        customAnalyzers[name] = analyzer
        logger.info { "注册自定义代码流分析器: $name" }
    }
    
    /**
     * 获取分析器
     *
     * @param type 流类型
     * @return 分析器，如果找不到则返回 null
     */
    fun getAnalyzer(type: FlowType): CodeFlowAnalyzer? {
        return analyzers[type]
    }
    
    /**
     * 获取自定义分析器
     *
     * @param name 分析器名称
     * @return 分析器，如果找不到则返回 null
     */
    fun getCustomAnalyzer(name: String): CodeFlowAnalyzer? {
        return customAnalyzers[name]
    }
    
    /**
     * 获取适合元素的分析器
     *
     * @param element 代码元素
     * @param type 流类型
     * @return 分析器，如果找不到则返回 null
     */
    fun getAnalyzerForElement(element: CodeElement, type: FlowType): CodeFlowAnalyzer? {
        val analyzer = getAnalyzer(type)
        if (analyzer != null && analyzer.supportsElement(element)) {
            return analyzer
        }
        
        // 尝试查找支持该元素的自定义分析器
        for (customAnalyzer in customAnalyzers.values) {
            if (customAnalyzer.supportsElement(element) && customAnalyzer.supportsFlowType(type)) {
                return customAnalyzer
            }
        }
        
        return null
    }
    
    /**
     * 清除所有分析器的缓存
     */
    fun clearCache() {
        analyzers.values.forEach { it.clearCache() }
        customAnalyzers.values.forEach { it.clearCache() }
        logger.info { "清除所有代码流分析器缓存" }
    }
}
