package ai.kastrax.server.common.model

import java.time.Instant

/**
 * 执行模型
 */
data class Execution(
    val id: String,
    val workflowId: String,
    val status: ExecutionStatus,
    val input: Map<String, Any>,
    val output: Map<String, Any>,
    val error: String?,
    val startTime: Instant,
    val endTime: Instant?,
    val nodeExecutions: Map<String, NodeExecution>
)

/**
 * 执行状态
 */
enum class ExecutionStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELED,
    SUSPENDED
}

/**
 * 节点执行模型
 */
data class NodeExecution(
    val nodeId: String,
    val status: ExecutionStatus,
    val input: Map<String, Any>,
    val output: Map<String, Any>,
    val error: String?,
    val startTime: Instant,
    val endTime: Instant?
)
