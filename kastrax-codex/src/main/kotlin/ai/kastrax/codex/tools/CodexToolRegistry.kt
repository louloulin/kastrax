package ai.kastrax.codex.tools

import ai.kastrax.core.tools.Tool
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

/**
 * Codex工具注册器，用于注册和管理所有IDE特定工具
 */
@Service(Service.Level.PROJECT)
class CodexToolRegistry(private val project: Project) {
    
    private val logger = Logger.getInstance(CodexToolRegistry::class.java)
    private val tools = mutableMapOf<String, Tool>()
    
    init {
        // 注册所有工具
        registerTools()
    }
    
    /**
     * 注册所有工具
     */
    private fun registerTools() {
        try {
            // 注册代码分析工具
            val codeAnalysisTool = CodeAnalysisTool(project).createTool()
            registerTool(codeAnalysisTool)
            
            // 注册符号查找工具
            val symbolSearchTool = SymbolSearchTool(project).createTool()
            registerTool(symbolSearchTool)
            
            // 注册Git操作工具
            val gitOperationTool = GitOperationTool(project).createTool()
            registerTool(gitOperationTool)
            
            logger.info("已注册 ${tools.size} 个工具")
        } catch (e: Exception) {
            logger.error("注册工具失败", e)
        }
    }
    
    /**
     * 注册工具
     */
    fun registerTool(tool: Tool) {
        tools[tool.id] = tool
        logger.info("已注册工具: ${tool.id}")
    }
    
    /**
     * 获取工具
     */
    fun getTool(id: String): Tool? {
        return tools[id]
    }
    
    /**
     * 获取所有工具
     */
    fun getAllTools(): Map<String, Tool> {
        return tools.toMap()
    }
    
    /**
     * 获取工具映射，用于智能体
     */
    fun getToolMap(): Map<String, Tool> {
        return tools.toMap()
    }
}
