package ai.kastrax.server.common.model

/**
 * 断点模型
 */
data class Breakpoint(
    val id: String,
    val workflowId: String,
    val nodeId: String,
    val condition: String?,
    val enabled: Boolean
)

/**
 * 调试会话模型
 */
data class DebugSession(
    val id: String,
    val workflowId: String,
    val executionId: String,
    val status: DebugSessionStatus,
    val breakpoints: List<Breakpoint>,
    val currentNodeId: String?
)

/**
 * 调试会话状态
 */
enum class DebugSessionStatus {
    ACTIVE,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELED
}
