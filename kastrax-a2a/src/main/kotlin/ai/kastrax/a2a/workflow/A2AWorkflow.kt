package ai.kastrax.a2a.workflow

import ai.kastrax.a2a.agent.A2AAgent
import ai.kastrax.a2a.client.A2AClient
import ai.kastrax.a2a.model.InvokeRequest
import ai.kastrax.a2a.model.InvokeResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 工作流状态
 */
enum class WorkflowState {
    /**
     * 初始化
     */
    INITIALIZED,
    
    /**
     * 运行中
     */
    RUNNING,
    
    /**
     * 已完成
     */
    COMPLETED,
    
    /**
     * 失败
     */
    FAILED,
    
    /**
     * 已取消
     */
    CANCELLED
}

/**
 * 工作流步骤
 */
data class WorkflowStep(
    /**
     * 步骤 ID
     */
    val id: String,
    
    /**
     * 步骤名称
     */
    val name: String,
    
    /**
     * 代理 ID
     */
    val agentId: String,
    
    /**
     * 能力 ID
     */
    val capabilityId: String,
    
    /**
     * 参数提供器
     */
    val parameterProvider: suspend (Map<String, JsonElement>) -> Map<String, JsonElement>,
    
    /**
     * 依赖步骤 ID 列表
     */
    val dependsOn: List<String> = emptyList(),
    
    /**
     * 重试次数
     */
    val retries: Int = 3,
    
    /**
     * 超时时间（毫秒）
     */
    val timeoutMs: Long = 30000
)

/**
 * 工作流步骤结果
 */
data class WorkflowStepResult(
    /**
     * 步骤 ID
     */
    val stepId: String,
    
    /**
     * 结果
     */
    val result: JsonElement,
    
    /**
     * 是否成功
     */
    val success: Boolean,
    
    /**
     * 错误消息
     */
    val errorMessage: String? = null
)

/**
 * 工作流事件
 */
sealed class WorkflowEvent {
    /**
     * 工作流 ID
     */
    abstract val workflowId: String
    
    /**
     * 工作流开始事件
     */
    data class Started(
        override val workflowId: String,
        val steps: List<WorkflowStep>
    ) : WorkflowEvent()
    
    /**
     * 步骤开始事件
     */
    data class StepStarted(
        override val workflowId: String,
        val stepId: String
    ) : WorkflowEvent()
    
    /**
     * 步骤完成事件
     */
    data class StepCompleted(
        override val workflowId: String,
        val stepId: String,
        val result: JsonElement
    ) : WorkflowEvent()
    
    /**
     * 步骤失败事件
     */
    data class StepFailed(
        override val workflowId: String,
        val stepId: String,
        val error: String
    ) : WorkflowEvent()
    
    /**
     * 工作流完成事件
     */
    data class Completed(
        override val workflowId: String,
        val results: Map<String, JsonElement>
    ) : WorkflowEvent()
    
    /**
     * 工作流失败事件
     */
    data class Failed(
        override val workflowId: String,
        val error: String
    ) : WorkflowEvent()
}

/**
 * A2A 工作流，用于管理代理间的复杂交互和任务流程
 */
class A2AWorkflow(
    /**
     * 工作流 ID
     */
    val id: String = UUID.randomUUID().toString(),
    
    /**
     * 工作流名称
     */
    val name: String,
    
    /**
     * 工作流描述
     */
    val description: String = "",
    
    /**
     * 本地代理映射
     */
    private val localAgents: Map<String, A2AAgent> = emptyMap(),
    
    /**
     * 远程代理客户端映射
     */
    private val remoteClients: Map<String, A2AClient> = emptyMap(),
    
    /**
     * 协程作用域
     */
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    /**
     * 工作流步骤列表
     */
    private val steps = mutableListOf<WorkflowStep>()
    
    /**
     * 步骤结果映射
     */
    private val stepResults = ConcurrentHashMap<String, WorkflowStepResult>()
    
    /**
     * 工作流状态
     */
    private var state = WorkflowState.INITIALIZED
    
    /**
     * 事件流
     */
    private val _events = MutableSharedFlow<WorkflowEvent>(replay = 0)
    
    /**
     * 事件流（只读）
     */
    val events: Flow<WorkflowEvent> = _events.asSharedFlow()
    
    /**
     * 添加工作流步骤
     */
    fun addStep(step: WorkflowStep) {
        steps.add(step)
    }
    
    /**
     * 添加工作流步骤
     */
    fun addStep(
        id: String,
        name: String,
        agentId: String,
        capabilityId: String,
        parameterProvider: suspend (Map<String, JsonElement>) -> Map<String, JsonElement>,
        dependsOn: List<String> = emptyList(),
        retries: Int = 3,
        timeoutMs: Long = 30000
    ) {
        addStep(
            WorkflowStep(
                id = id,
                name = name,
                agentId = agentId,
                capabilityId = capabilityId,
                parameterProvider = parameterProvider,
                dependsOn = dependsOn,
                retries = retries,
                timeoutMs = timeoutMs
            )
        )
    }
    
    /**
     * 执行工作流
     */
    suspend fun execute(): Map<String, JsonElement> {
        if (state == WorkflowState.RUNNING) {
            throw IllegalStateException("Workflow is already running")
        }
        
        state = WorkflowState.RUNNING
        stepResults.clear()
        
        try {
            // 发送工作流开始事件
            _events.emit(WorkflowEvent.Started(id, steps))
            
            // 创建步骤依赖图
            val stepMap = steps.associateBy { it.id }
            val remainingSteps = steps.toMutableList()
            val results = mutableMapOf<String, JsonElement>()
            
            // 执行工作流，直到所有步骤完成或失败
            while (remainingSteps.isNotEmpty() && state == WorkflowState.RUNNING) {
                // 找出可以执行的步骤（所有依赖都已完成）
                val executableSteps = remainingSteps.filter { step ->
                    step.dependsOn.all { dependencyId -> results.containsKey(dependencyId) }
                }
                
                if (executableSteps.isEmpty()) {
                    // 如果没有可执行的步骤，但仍有剩余步骤，说明存在循环依赖
                    throw IllegalStateException("Circular dependency detected in workflow")
                }
                
                // 并行执行可执行的步骤
                val deferredResults = executableSteps.map { step ->
                    scope.async {
                        executeStep(step, results)
                    }
                }
                
                // 等待所有步骤完成
                deferredResults.forEach { it.await() }
                
                // 从剩余步骤中移除已执行的步骤
                remainingSteps.removeAll(executableSteps)
                
                // 更新结果映射
                stepResults.forEach { (stepId, result) ->
                    if (result.success) {
                        results[stepId] = result.result
                    } else {
                        throw RuntimeException("Step $stepId failed: ${result.errorMessage}")
                    }
                }
            }
            
            // 发送工作流完成事件
            state = WorkflowState.COMPLETED
            _events.emit(WorkflowEvent.Completed(id, results))
            
            return results
        } catch (e: Exception) {
            // 发送工作流失败事件
            state = WorkflowState.FAILED
            _events.emit(WorkflowEvent.Failed(id, e.message ?: "Unknown error"))
            
            throw e
        }
    }
    
    /**
     * 取消工作流
     */
    fun cancel() {
        if (state == WorkflowState.RUNNING) {
            state = WorkflowState.CANCELLED
        }
    }
    
    /**
     * 获取工作流状态
     */
    fun getState(): WorkflowState = state
    
    /**
     * 获取步骤结果
     */
    fun getStepResult(stepId: String): WorkflowStepResult? = stepResults[stepId]
    
    /**
     * 获取所有步骤结果
     */
    fun getAllStepResults(): Map<String, WorkflowStepResult> = stepResults.toMap()
    
    /**
     * 执行单个工作流步骤
     */
    private suspend fun executeStep(step: WorkflowStep, previousResults: Map<String, JsonElement>): WorkflowStepResult {
        // 发送步骤开始事件
        _events.emit(WorkflowEvent.StepStarted(id, step.id))
        
        try {
            // 获取参数
            val parameters = step.parameterProvider(previousResults)
            
            // 创建调用请求
            val request = InvokeRequest(
                id = UUID.randomUUID().toString(),
                capabilityId = step.capabilityId,
                parameters = parameters
            )
            
            // 调用代理能力
            val response = invokeAgent(step.agentId, request)
            
            // 创建步骤结果
            val stepResult = WorkflowStepResult(
                stepId = step.id,
                result = response.result,
                success = true
            )
            
            // 保存步骤结果
            stepResults[step.id] = stepResult
            
            // 发送步骤完成事件
            _events.emit(WorkflowEvent.StepCompleted(id, step.id, response.result))
            
            return stepResult
        } catch (e: Exception) {
            // 创建步骤失败结果
            val stepResult = WorkflowStepResult(
                stepId = step.id,
                result = JsonPrimitive(""),
                success = false,
                errorMessage = e.message
            )
            
            // 保存步骤结果
            stepResults[step.id] = stepResult
            
            // 发送步骤失败事件
            _events.emit(WorkflowEvent.StepFailed(id, step.id, e.message ?: "Unknown error"))
            
            return stepResult
        }
    }
    
    /**
     * 调用代理能力
     */
    private suspend fun invokeAgent(agentId: String, request: InvokeRequest): InvokeResponse {
        // 首先尝试本地代理
        val localAgent = localAgents[agentId]
        if (localAgent != null) {
            return localAgent.invoke(request)
        }
        
        // 然后尝试远程代理
        val remoteClient = remoteClients[agentId]
        if (remoteClient != null) {
            return remoteClient.invoke(agentId, request.capabilityId, request.parameters)
        }
        
        throw IllegalArgumentException("Agent not found: $agentId")
    }
}

/**
 * 工作流 DSL 构建器
 */
class WorkflowBuilder {
    /**
     * 工作流 ID
     */
    var id: String = UUID.randomUUID().toString()
    
    /**
     * 工作流名称
     */
    var name: String = "A2A Workflow"
    
    /**
     * 工作流描述
     */
    var description: String = ""
    
    /**
     * 本地代理映射
     */
    private val localAgents = mutableMapOf<String, A2AAgent>()
    
    /**
     * 远程代理客户端映射
     */
    private val remoteClients = mutableMapOf<String, A2AClient>()
    
    /**
     * 工作流步骤列表
     */
    private val steps = mutableListOf<WorkflowStep>()
    
    /**
     * 添加本地代理
     */
    fun localAgent(id: String, agent: A2AAgent) {
        localAgents[id] = agent
    }
    
    /**
     * 添加远程代理客户端
     */
    fun remoteAgent(id: String, client: A2AClient) {
        remoteClients[id] = client
    }
    
    /**
     * 添加工作流步骤
     */
    fun step(init: StepBuilder.() -> Unit) {
        val builder = StepBuilder()
        builder.init()
        steps.add(builder.build())
    }
    
    /**
     * 构建工作流
     */
    fun build(): A2AWorkflow {
        val workflow = A2AWorkflow(
            id = id,
            name = name,
            description = description,
            localAgents = localAgents,
            remoteClients = remoteClients
        )
        
        steps.forEach { workflow.addStep(it) }
        
        return workflow
    }
}

/**
 * 工作流步骤 DSL 构建器
 */
class StepBuilder {
    /**
     * 步骤 ID
     */
    var id: String = UUID.randomUUID().toString()
    
    /**
     * 步骤名称
     */
    var name: String = ""
    
    /**
     * 代理 ID
     */
    var agentId: String = ""
    
    /**
     * 能力 ID
     */
    var capabilityId: String = ""
    
    /**
     * 依赖步骤 ID 列表
     */
    private val dependsOn = mutableListOf<String>()
    
    /**
     * 重试次数
     */
    var retries: Int = 3
    
    /**
     * 超时时间（毫秒）
     */
    var timeoutMs: Long = 30000
    
    /**
     * 参数提供器
     */
    private var parameterProvider: (suspend (Map<String, JsonElement>) -> Map<String, JsonElement>)? = null
    
    /**
     * 添加依赖步骤
     */
    fun dependsOn(stepId: String) {
        dependsOn.add(stepId)
    }
    
    /**
     * 设置参数提供器
     */
    fun parameters(provider: suspend (Map<String, JsonElement>) -> Map<String, JsonElement>) {
        parameterProvider = provider
    }
    
    /**
     * 构建工作流步骤
     */
    fun build(): WorkflowStep {
        require(name.isNotBlank()) { "Step name is required" }
        require(agentId.isNotBlank()) { "Agent ID is required" }
        require(capabilityId.isNotBlank()) { "Capability ID is required" }
        requireNotNull(parameterProvider) { "Parameter provider is required" }
        
        return WorkflowStep(
            id = id,
            name = name,
            agentId = agentId,
            capabilityId = capabilityId,
            parameterProvider = parameterProvider!!,
            dependsOn = dependsOn,
            retries = retries,
            timeoutMs = timeoutMs
        )
    }
}

/**
 * 创建工作流的 DSL 函数
 */
fun workflow(init: WorkflowBuilder.() -> Unit): A2AWorkflow {
    val builder = WorkflowBuilder()
    builder.init()
    return builder.build()
}
