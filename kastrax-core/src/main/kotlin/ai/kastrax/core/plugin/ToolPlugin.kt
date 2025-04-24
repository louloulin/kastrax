package ai.kastrax.core.plugin

import ai.kastrax.core.tools.Tool

/**
 * 工具插件接口，用于提供自定义工具。
 */
interface ToolPlugin : Plugin {
    /**
     * 获取此插件提供的工具列表。
     *
     * @return 工具列表
     */
    fun getTools(): List<Tool>
    
    /**
     * 获取工具。
     *
     * @param toolId 工具ID
     * @return 工具实例，如果不存在则返回null
     */
    fun getTool(toolId: String): Tool?
}

/**
 * 抽象工具插件基类，提供了ToolPlugin接口的基本实现。
 */
abstract class AbstractToolPlugin(
    id: String,
    name: String,
    description: String,
    version: String,
    author: String,
    config: PluginConfig = DefaultPluginConfig()
) : AbstractPlugin(
    id = id,
    name = name,
    description = description,
    version = version,
    author = author,
    type = PluginType.TOOL,
    config = config
), ToolPlugin {
    
    private val tools = mutableMapOf<String, Tool>()
    
    /**
     * 注册工具。
     *
     * @param tool 工具实例
     */
    protected fun registerTool(tool: Tool) {
        tools[tool.id] = tool
        logger.info { "注册工具: ${tool.name} (${tool.id})" }
    }
    
    /**
     * 注销工具。
     *
     * @param toolId 工具ID
     */
    protected fun unregisterTool(toolId: String) {
        val tool = tools.remove(toolId)
        if (tool != null) {
            logger.info { "注销工具: ${tool.name} (${tool.id})" }
        }
    }
    
    override fun getTools(): List<Tool> {
        return tools.values.toList()
    }
    
    override fun getTool(toolId: String): Tool? {
        return tools[toolId]
    }
}
