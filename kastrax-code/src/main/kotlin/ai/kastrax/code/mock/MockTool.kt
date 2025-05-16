package ai.kastrax.code.mock

import kotlin.reflect.KClass

/**
 * 模拟工具注解
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class tool(
    val name: String,
    val description: String = ""
)

/**
 * 模拟工具接口
 */
interface Tool {
    /**
     * 工具名称
     */
    val name: String
    
    /**
     * 工具描述
     */
    val description: String
    
    /**
     * 执行工具
     *
     * @param input 输入参数
     * @param metadata 元数据
     * @return 执行结果
     */
    suspend fun execute(input: Map<String, Any>, metadata: Map<String, Any> = emptyMap()): String
}

/**
 * 模拟工具工厂
 */
object ToolFactory {
    /**
     * 创建工具
     *
     * @param toolClass 工具类
     * @return 工具实例
     */
    fun <T : Tool> create(toolClass: KClass<T>): T {
        return toolClass.java.getDeclaredConstructor().newInstance()
    }
}

/**
 * 模拟工具注册表
 */
object ToolRegistry {
    private val tools = mutableMapOf<String, Tool>()
    
    /**
     * 注册工具
     *
     * @param tool 工具实例
     */
    fun register(tool: Tool) {
        tools[tool.name] = tool
    }
    
    /**
     * 注册工具类
     *
     * @param toolClass 工具类
     */
    fun <T : Tool> register(toolClass: KClass<T>) {
        val tool = ToolFactory.create(toolClass)
        register(tool)
    }
    
    /**
     * 获取工具
     *
     * @param name 工具名称
     * @return 工具实例
     */
    fun get(name: String): Tool? {
        return tools[name]
    }
    
    /**
     * 获取所有工具
     *
     * @return 所有工具
     */
    fun getAll(): List<Tool> {
        return tools.values.toList()
    }
    
    /**
     * 清空注册表
     */
    fun clear() {
        tools.clear()
    }
}
