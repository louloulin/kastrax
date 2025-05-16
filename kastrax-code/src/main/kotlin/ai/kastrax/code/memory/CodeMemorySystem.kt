package ai.kastrax.code.memory

import ai.kastrax.code.model.Context
import ai.kastrax.code.model.ContextElement
import ai.kastrax.memory.api.Memory
import ai.kastrax.memory.api.MemoryType

/**
 * 代码记忆系统接口
 *
 * 负责存储和检索代码相关的记忆，提高代码智能体的性能
 */
interface CodeMemorySystem {
    /**
     * 存储对话记忆
     *
     * @param conversationId 对话ID
     * @param memory 记忆
     * @return 是否成功存储
     */
    suspend fun storeConversationMemory(conversationId: String, memory: Memory): Boolean
    
    /**
     * 检索对话记忆
     *
     * @param conversationId 对话ID
     * @param limit 限制数量
     * @return 记忆列表
     */
    suspend fun retrieveConversationMemory(conversationId: String, limit: Int = 10): List<Memory>
    
    /**
     * 存储代码上下文记忆
     *
     * @param context 上下文
     * @return 是否成功存储
     */
    suspend fun storeCodeContextMemory(context: Context): Boolean
    
    /**
     * 检索代码上下文记忆
     *
     * @param query 查询字符串
     * @param limit 限制数量
     * @param minScore 最小相似度分数
     * @return 上下文元素列表
     */
    suspend fun retrieveCodeContextMemory(query: String, limit: Int = 10, minScore: Double = 0.0): List<ContextElement>
    
    /**
     * 存储项目记忆
     *
     * @param projectId 项目ID
     * @param memory 记忆
     * @return 是否成功存储
     */
    suspend fun storeProjectMemory(projectId: String, memory: Memory): Boolean
    
    /**
     * 检索项目记忆
     *
     * @param projectId 项目ID
     * @param memoryType 记忆类型
     * @param limit 限制数量
     * @return 记忆列表
     */
    suspend fun retrieveProjectMemory(projectId: String, memoryType: MemoryType? = null, limit: Int = 10): List<Memory>
    
    /**
     * 存储用户偏好记忆
     *
     * @param userId 用户ID
     * @param key 键
     * @param value 值
     * @return 是否成功存储
     */
    suspend fun storeUserPreferenceMemory(userId: String, key: String, value: String): Boolean
    
    /**
     * 检索用户偏好记忆
     *
     * @param userId 用户ID
     * @param key 键
     * @return 值
     */
    suspend fun retrieveUserPreferenceMemory(userId: String, key: String): String?
    
    /**
     * 清除对话记忆
     *
     * @param conversationId 对话ID
     * @return 是否成功清除
     */
    suspend fun clearConversationMemory(conversationId: String): Boolean
    
    /**
     * 清除代码上下文记忆
     *
     * @return 是否成功清除
     */
    suspend fun clearCodeContextMemory(): Boolean
    
    /**
     * 清除项目记忆
     *
     * @param projectId 项目ID
     * @return 是否成功清除
     */
    suspend fun clearProjectMemory(projectId: String): Boolean
    
    /**
     * 清除用户偏好记忆
     *
     * @param userId 用户ID
     * @return 是否成功清除
     */
    suspend fun clearUserPreferenceMemory(userId: String): Boolean
    
    /**
     * 关闭记忆系统
     */
    suspend fun close()
}
