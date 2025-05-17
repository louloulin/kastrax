package ai.kastrax.code.tools

import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.tool
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
        logger.debug("注册工具: ${tool.name}")
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
        logger.debug("清除所有工具")
    }

    /**
     * 初始化默认工具
     */
    fun initializeDefaultTools() {
        // 添加代码格式化工具
        val formatCodeTool = tool {
            id = "format_code"
            name = "代码格式化"
            description = "格式化代码"

            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("code", buildJsonObject {
                        put("type", "string")
                        put("description", "要格式化的代码")
                    })
                    put("language", buildJsonObject {
                        put("type", "string")
                        put("description", "代码语言")
                    })
                })
                put("required", buildJsonObject {
                    put("code", true)
                    put("language", true)
                })
            }

            execute = { input ->
                withContext(Dispatchers.IO) {
                    try {
                        val jsonObject = input as? JsonObject
                            ?: throw IllegalArgumentException("输入必须是 JSON 对象")

                        val code = (jsonObject["code"] as? JsonPrimitive)?.content
                            ?: throw IllegalArgumentException("缺少 'code' 参数")

                        val language = (jsonObject["language"] as? JsonPrimitive)?.content
                            ?: throw IllegalArgumentException("缺少 'language' 参数")

                        // 这里可以添加实际的代码格式化逻辑
                        // 现在只是简单返回原始代码
                        buildJsonObject {
                            put("formattedCode", code)
                            put("language", language)
                            put("success", true)
                        }
                    } catch (e: Exception) {
                        logger.error("格式化代码失败", e)
                        buildJsonObject {
                            put("error", e.message ?: "未知错误")
                            put("success", false)
                        }
                    }
                }
            }
        }
        registerTool(formatCodeTool)

        // 添加代码分析工具
        val analyzeCodeTool = tool {
            id = "analyze_code"
            name = "代码分析"
            description = "分析代码并提供改进建议"

            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("code", buildJsonObject {
                        put("type", "string")
                        put("description", "要分析的代码")
                    })
                    put("language", buildJsonObject {
                        put("type", "string")
                        put("description", "代码语言")
                    })
                })
                put("required", buildJsonObject {
                    put("code", true)
                    put("language", true)
                })
            }

            execute = { input ->
                withContext(Dispatchers.IO) {
                    try {
                        val jsonObject = input as? JsonObject
                            ?: throw IllegalArgumentException("输入必须是 JSON 对象")

                        val code = (jsonObject["code"] as? JsonPrimitive)?.content
                            ?: throw IllegalArgumentException("缺少 'code' 参数")

                        val language = (jsonObject["language"] as? JsonPrimitive)?.content
                            ?: throw IllegalArgumentException("缺少 'language' 参数")

                        // 这里可以添加实际的代码分析逻辑
                        // 现在只是返回简单的分析结果
                        buildJsonObject {
                            put("suggestions", "没有发现问题，代码看起来很好。")
                            put("language", language)
                            put("success", true)
                        }
                    } catch (e: Exception) {
                        logger.error("分析代码失败", e)
                        buildJsonObject {
                            put("error", e.message ?: "未知错误")
                            put("success", false)
                        }
                    }
                }
            }
        }
        registerTool(analyzeCodeTool)
    }

    companion object {
        /**
         * 获取实例
         *
         * @param project 项目
         * @return 代码工具注册表实例
         */
        fun getInstance(project: Project): CodeToolRegistry {
            return project.service<CodeToolRegistry>()
        }
    }
}
