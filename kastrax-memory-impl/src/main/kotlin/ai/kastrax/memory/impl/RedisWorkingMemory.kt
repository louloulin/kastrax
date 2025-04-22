package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.tools.Tool
import ai.kastrax.memory.api.UpdateWorkingMemoryParams
import ai.kastrax.memory.api.UpdateWorkingMemoryResult
import ai.kastrax.memory.api.WorkingMemory
import ai.kastrax.memory.api.WorkingMemoryConfig
import ai.kastrax.memory.api.WorkingMemoryMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import redis.clients.jedis.JedisPool
import java.util.concurrent.TimeUnit

/**
 * Redis实现的工作内存。
 *
 * @property jedisPool Redis连接池
 * @property keyPrefix Redis键前缀
 * @property expireTime 过期时间（秒）
 */
class RedisWorkingMemory(
    private val jedisPool: JedisPool,
    private val keyPrefix: String = "kastrax:working_memory:",
    private val expireTime: Long = TimeUnit.DAYS.toSeconds(30) // 默认30天过期
) : WorkingMemory, KastraXBase(component = "WORKING_MEMORY", name = "redis") {
    
    /**
     * 构建Redis键。
     */
    private fun buildKey(threadId: String): String {
        return "$keyPrefix$threadId"
    }
    
    override suspend fun getWorkingMemory(threadId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    val key = buildKey(threadId)
                    jedis.get(key)
                }
            } catch (e: Exception) {
                logger.error("从Redis获取工作内存失败: ${e.message}")
                null
            }
        }
    }
    
    override suspend fun updateWorkingMemory(threadId: String, content: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                jedisPool.resource.use { jedis ->
                    val key = buildKey(threadId)
                    jedis.setex(key, expireTime, content)
                    true
                }
            } catch (e: Exception) {
                logger.error("更新Redis工作内存失败: ${e.message}")
                false
            }
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
        
        val updateWorkingMemoryTool = ai.kastrax.core.tools.tool {
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
                put("required", listOf("memory"))
            }
            execute = { input ->
                val params = try {
                    val memory = input.toString()
                    UpdateWorkingMemoryParams(memory)
                } catch (e: Exception) {
                    logger.error("解析工作内存参数失败: ${e.message}")
                    return@tool buildJsonObject {
                        put("success", false)
                        put("error", "解析工作内存参数失败: ${e.message}")
                    }
                }
                
                val threadId = input.toString().substringAfter("threadId=").substringBefore(",")
                if (threadId.isBlank()) {
                    return@tool buildJsonObject {
                        put("success", false)
                        put("error", "未提供线程ID")
                    }
                }
                
                try {
                    val success = updateWorkingMemory(threadId, params.memory)
                    val result = UpdateWorkingMemoryResult(success)
                    
                    buildJsonObject {
                        put("success", result.success)
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
