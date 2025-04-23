package ai.kastrax.core.workflow.state

import ai.kastrax.core.common.KastraXBase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging

/**
 * 内存中的工作流状态存储实现。
 *
 * 注意：此实现不持久化数据，应用重启后数据会丢失。
 */
class InMemoryWorkflowStateStorage : WorkflowStateStorage, KastraXBase(component = "WORKFLOW_STATE_STORAGE", name = "in-memory") {
    private val storageLogger = KotlinLogging.logger {}
    private val mutex = Mutex()
    private val states = mutableMapOf<Pair<String, String>, WorkflowState>()

    override suspend fun saveWorkflowState(workflowId: String, runId: String, state: WorkflowState): Boolean {
        return mutex.withLock {
            val key = workflowId to runId
            val updatedState = state.copy(updatedAt = System.currentTimeMillis())
            states[key] = updatedState
            storageLogger.debug { "保存工作流状态: $workflowId, $runId, 状态: ${state.status}" }
            true
        }
    }

    override suspend fun getWorkflowState(workflowId: String, runId: String): WorkflowState? {
        return mutex.withLock {
            val key = workflowId to runId
            val state = states[key]
            if (state == null) {
                storageLogger.debug { "未找到工作流状态: $workflowId, $runId" }
            } else {
                storageLogger.debug { "获取工作流状态: $workflowId, $runId, 状态: ${state.status}" }
            }
            state
        }
    }

    override suspend fun getWorkflowRuns(workflowId: String, limit: Int, offset: Int): List<WorkflowRunInfo> {
        return mutex.withLock {
            states.entries
                .filter { (key, _) -> key.first == workflowId }
                .map { (_, state) ->
                    WorkflowRunInfo(
                        runId = state.runId,
                        workflowId = state.workflowId,
                        status = state.status,
                        createdAt = state.createdAt,
                        updatedAt = state.updatedAt
                    )
                }
                .sortedByDescending { it.updatedAt }
                .drop(offset)
                .take(limit)
        }
    }

    override suspend fun deleteWorkflowState(workflowId: String, runId: String): Boolean {
        return mutex.withLock {
            val key = workflowId to runId
            val removed = states.remove(key) != null
            if (removed) {
                storageLogger.debug { "删除工作流状态: $workflowId, $runId" }
            } else {
                storageLogger.debug { "尝试删除不存在的工作流状态: $workflowId, $runId" }
            }
            removed
        }
    }
}
