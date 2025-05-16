package ai.kastrax.code.tools

import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.core.tool.Tool
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

/**
 * 代码工具注册表
 *
 * 管理和提供代码工具
 */
@Service(Service.Level.PROJECT)
class CodeToolRegistry(
    private val project: Project
) : KastraXCodeBase("TOOL_REGISTRY") {
    private val tools = mutableMapOf<String, Tool>()

    /**
     * 注册工具
     *
     * @param tool 工具实例
     */
    fun registerTool(tool: Tool) {
        tools[tool.name] = tool
        debug("注册工具: ${tool.name}")
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
     * 获取指定名称的工具
     *
     * @param name 工具名称
     * @return 工具实例，如果不存在则返回null
     */
    fun getToolByName(name: String): Tool? {
        return tools[name]
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
     * 获取所有工具的名称
     *
     * @return 工具名称列表
     */
    fun getToolNames(): List<String> {
        return tools.keys.toList()
    }

    /**
     * 清除所有工具
     */
    fun clear() {
        tools.clear()
        debug("清除所有工具")
    }

    companion object {
        /**
         * 获取实例
         *
         * @param project 项目
         * @return 代码工具注册表实例
         */
        fun getInstance(project: Project): CodeToolRegistry {
            return project.getService(CodeToolRegistry::class.java)
        }
    }
}
