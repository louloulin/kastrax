package ai.kastrax.memory.api

import kotlinx.serialization.Serializable

/**
 * 工作内存模式枚举。
 */
enum class WorkingMemoryMode {
    /**
     * 文本流模式：工作内存作为系统消息的一部分发送给模型。
     */
    TEXT_STREAM,

    /**
     * 工具调用模式：提供一个工具让模型更新工作内存。
     */
    TOOL_CALL
}

/**
 * 工作内存配置。
 *
 * @property enabled 是否启用工作内存
 * @property mode 工作内存模式
 * @property template 工作内存模板
 */
@Serializable
data class WorkingMemoryConfig(
    val enabled: Boolean = false,
    val mode: WorkingMemoryMode = WorkingMemoryMode.TEXT_STREAM,
    val template: String = DEFAULT_WORKING_MEMORY_TEMPLATE
) {
    companion object {
        /**
         * 默认工作内存模板。
         */
        val DEFAULT_WORKING_MEMORY_TEMPLATE = """
            # 用户信息
            - 姓名: 未知
            - 位置: 未知
            - 偏好: 未知

            # 对话上下文
            - 主题: 未知
            - 目标: 未知

            # 重要信息
            -
        """.trimIndent()
    }
}

/**
 * 工作内存接口，用于管理Agent的工作内存。
 */
interface WorkingMemory {
    /**
     * 获取工作内存内容。
     *
     * @param threadId 线程ID
     * @return 工作内存内容
     */
    suspend fun getWorkingMemory(threadId: String): String?

    /**
     * 更新工作内存内容。
     *
     * @param threadId 线程ID
     * @param content 新的工作内存内容
     * @return 是否成功更新
     */
    suspend fun updateWorkingMemory(threadId: String, content: String): Boolean

    /**
     * 获取工作内存系统消息。
     *
     * @param threadId 线程ID
     * @param config 工作内存配置
     * @return 系统消息
     */
    suspend fun getSystemMessage(threadId: String, config: WorkingMemoryConfig? = null): String?

    /**
     * 获取工作内存工具。
     *
     * @param config 工作内存配置
     * @return 工作内存工具
     */
    fun getTools(config: WorkingMemoryConfig? = null): Map<String, Any>
}

/**
 * 工作内存工具参数。
 *
 * @property memory 工作内存内容
 */
@Serializable
data class UpdateWorkingMemoryParams(
    val memory: String
)

/**
 * 工作内存工具结果。
 *
 * @property success 是否成功
 */
@Serializable
data class UpdateWorkingMemoryResult(
    val success: Boolean
)
