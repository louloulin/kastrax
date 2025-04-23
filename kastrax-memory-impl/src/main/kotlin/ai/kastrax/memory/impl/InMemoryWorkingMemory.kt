package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.tool
import ai.kastrax.memory.api.UpdateWorkingMemoryParams
import ai.kastrax.memory.api.UpdateWorkingMemoryResult
import ai.kastrax.memory.api.WorkingMemory
import ai.kastrax.memory.api.WorkingMemoryConfig
import ai.kastrax.memory.api.WorkingMemoryMode
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * 内存中的工作内存实现。
 */
class InMemoryWorkingMemory : WorkingMemory, KastraXBase(component = "WORKING_MEMORY", name = "in-memory") {
    private val mutex = Mutex()
    private val memories = mutableMapOf<String, String>()

    override suspend fun getWorkingMemory(threadId: String): String? {
        return mutex.withLock {
            memories[threadId]
        }
    }

    override suspend fun updateWorkingMemory(threadId: String, content: String): Boolean {
        return mutex.withLock {
            memories[threadId] = content
            true
        }
    }

    override suspend fun getSystemMessage(threadId: String, config: WorkingMemoryConfig?): String? {
        val cfg = config ?: WorkingMemoryConfig()
        if (!cfg.enabled) return null

        val memory = getWorkingMemory(threadId) ?: cfg.template

        return when (cfg.mode) {
            WorkingMemoryMode.TEXT_STREAM -> {
                """
                # 工作内存
                以下是你的工作内存，包含关于用户和对话的重要信息。请在回答时参考这些信息。

                $memory
                """.trimIndent()
            }
            WorkingMemoryMode.TOOL_CALL -> {
                """
                # 工作内存
                你可以使用update_working_memory工具来更新工作内存。当前工作内存内容：

                $memory
                """.trimIndent()
            }
        }
    }

    override fun getTools(config: WorkingMemoryConfig?): Map<String, Tool> {
        val cfg = config ?: WorkingMemoryConfig()
        if (!cfg.enabled || cfg.mode != WorkingMemoryMode.TOOL_CALL) {
            return emptyMap()
        }

        val updateWorkingMemoryTool = tool {
            id = "update_working_memory"
            name = "更新工作内存"
            description = "更新工作内存中的信息"
            inputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("memory") {
                        put("type", "string")
                        put("description", "新的工作内存内容")
                    }
                }
                put("required", JsonArray(listOf(JsonPrimitive("memory"))))
            }
            execute = { input ->
                // 解析输入
                val inputStr = input.toString()
                val threadId = inputStr.substringAfter("threadId=").substringBefore(",")

                // 检查线程ID
                if (threadId.isBlank()) {
                    buildJsonObject {
                        put("success", false)
                        put("error", "未提供线程ID")
                    }
                }

                // 解析内存内容
                val memoryContent = try {
                    // 从输入中提取memory参数
                    val memoryParam = if (inputStr.contains("memory=")) {
                        inputStr.substringAfter("memory=").substringBefore(",")
                    } else {
                        inputStr // 假设整个输入就是内存内容
                    }

                    memoryParam
                } catch (e: Exception) {
                    logger.error("解析工作内存参数失败: ${e.message}")
                    buildJsonObject {
                        put("success", false)
                        put("error", "解析工作内存参数失败: ${e.message}")
                    }
                }

                // 更新工作内存
                try {
                    val success = updateWorkingMemory(threadId, memoryContent.toString())

                    buildJsonObject {
                        put("success", success)
                    }
                } catch (e: Exception) {
                    logger.error("更新工作内存失败: ${e.message}")
                    buildJsonObject {
                        put("success", false)
                        put("error", "更新工作内存失败: ${e.message}")
                    }
                }
            }
        }

        return mapOf("update_working_memory" to updateWorkingMemoryTool)
    }
}
