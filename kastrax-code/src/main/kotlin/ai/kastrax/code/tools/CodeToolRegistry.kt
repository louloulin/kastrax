package ai.kastrax.code.tools

import ai.kastrax.core.tool.Tool
import io.github.oshai.kotlinlogging.KotlinLogging

/**
 * 代码工具注册表
 * 
 * 管理和提供代码工具
 */
class CodeToolRegistry {
    private val logger = KotlinLogging.logger {}
    private val tools = mutableMapOf<String, Tool>()
    
    /**
     * 注册工具
     *
     * @param tool 工具实例
     */
    fun registerTool(tool: Tool) {
        val id = when (tool) {
            is CodeTool -> tool.id
            else -> tool.javaClass.simpleName
        }
        
        logger.debug { "注册工具: $id" }
        tools[id] = tool
    }
    
    /**
     * 获取所有工具
     *
     * @return 工具列表
     */
    fun getTools(): List<Tool> {
        return tools.values.toList()
    }
    
    /**
     * 获取指定ID的工具
     *
     * @param id 工具ID
     * @return 工具实例，如果不存在则返回null
     */
    fun getToolById(id: String): Tool? {
        return tools[id]
    }
    
    /**
     * 获取特定类型的工具
     *
     * @param T 工具类型
     * @return 指定类型的工具列表
     */
    inline fun <reified T : Tool> getToolsByType(): List<T> {
        return tools.values.filterIsInstance<T>()
    }
    
    /**
     * 清除所有工具
     */
    fun clear() {
        tools.clear()
    }
    
    companion object {
        /**
         * 创建默认工具注册表
         *
         * @return 包含默认工具的注册表
         */
        fun createDefault(): CodeToolRegistry {
            val registry = CodeToolRegistry()
            // 在这里注册默认工具
            return registry
        }
    }
}
