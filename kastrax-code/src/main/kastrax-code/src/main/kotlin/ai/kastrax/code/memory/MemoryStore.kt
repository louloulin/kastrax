package ai.kastrax.code.memory

import ai.kastrax.code.model.ContextElement
import ai.kastrax.code.model.SimpleMemory

/**
 * 内存存储接口
 *
 * 提供存储和检索内存的方法
 */
interface MemoryStore {
    /**
     * 存储内存
     *
     * @param type 内存类型
     * @param namespace 命名空间
     * @param key 键
     * @param value 值
     * @param metadata 元数据
     * @return 内存ID
     */
    suspend fun storeMemory(
        type: MemoryType,
        namespace: String,
        key: String,
        value: String,
        metadata: Map<String, String> = emptyMap()
    ): String

    /**
     * 查询内存
     *
     * @param type 内存类型
     * @param namespace 命名空间
     * @param query 查询条件
     * @param limit 返回的最大结果数量
     * @return 内存列表
     */
    suspend fun queryMemories(
        type: MemoryType,
        namespace: String,
        query: String,
        limit: Int = 10
    ): List<SimpleMemory>

    /**
     * 删除内存
     *
     * @param id 内存ID
     * @return 是否成功删除
     */
    suspend fun deleteMemory(id: String): Boolean

    /**
     * 关闭存储
     */
    suspend fun close()
}

/**
 * 内存类型枚举
 */
enum class MemoryType {
    /**
     * 对话内存
     */
    CONVERSATION,

    /**
     * 代码上下文内存
     */
    CODE_CONTEXT,

    /**
     * 语义内存
     */
    SEMANTIC,

    /**
     * 命名空间
     */
    NAMESPACE,

    /**
     * 偏好设置
     */
    PREFERENCE
}
