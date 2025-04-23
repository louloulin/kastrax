package ai.kastrax.examples.workflow

import ai.kastrax.core.agent.agent
import ai.kastrax.core.workflow.*
import ai.kastrax.core.workflow.builder.workflow
import ai.kastrax.core.workflow.callback.StepCallback
import ai.kastrax.core.workflow.callback.WorkflowCallback
import ai.kastrax.core.workflow.engine.EventAwareWorkflowEngine
import ai.kastrax.core.workflow.event.*
import ai.kastrax.core.workflow.state.InMemoryWorkflowStateStorage
import ai.kastrax.integrations.openai.openAi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 工作流事件和回调示例，展示如何使用事件和回调机制。
 */
fun main() = runBlocking {
    println("开始工作流事件和回调示例...")
    
    // 创建OpenAI客户端
    val openAi = openAi {
        apiKey = System.getenv("OPENAI_API_KEY") ?: ""
    }
    
    // 创建Agent
    val agent = agent(openAi) {
        model = "gpt-3.5-turbo"
    }
    
    // 创建事件总线
    val eventBus = DefaultWorkflowEventBus()
    
    // 创建事件存储
    val eventStorage = InMemoryWorkflowEventStorage()
    
    // 创建状态存储
    val stateStorage = InMemoryWorkflowStateStorage()
    
    // 创建事件监听器
    val eventListener = object : WorkflowEventListener {
        override suspend fun onEvent(event: WorkflowEvent) {
            val timestamp = Instant.ofEpochMilli(event.timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
            
            println("📣 事件: ${event.type} - 时间: $timestamp")
            
            when (event) {
                is WorkflowStartedEvent -> {
                    println("  🚀 工作流开始: ${event.workflowId}, 运行ID: ${event.runId}")
                    println("  📥 输入: ${event.input}")
                }
                is WorkflowCompletedEvent -> {
                    println("  ✅ 工作流完成: ${event.workflowId}, 运行ID: ${event.runId}")
                    println("  📤 输出: ${event.output}")
                    println("  ⏱️ 执行时间: ${event.executionTime}ms")
                }
                is WorkflowFailedEvent -> {
                    println("  ❌ 工作流失败: ${event.workflowId}, 运行ID: ${event.runId}")
                    println("  🔴 错误: ${event.error}")
                }
                is StepStartedEvent -> {
                    println("  🔄 步骤开始: ${event.stepId}")
                    println("  📥 输入: ${event.input}")
                }
                is StepCompletedEvent -> {
                    println("  ✅ 步骤完成: ${event.stepId}")
                    println("  📤 输出: ${event.result.output}")
                    println("  ⏱️ 执行时间: ${event.result.executionTime}ms")
                }
                is StepFailedEvent -> {
                    println("  ❌ 步骤失败: ${event.stepId}")
                    println("  🔴 错误: ${event.error}")
                }
                is StepSkippedEvent -> {
                    println("  ⏭️ 步骤跳过: ${event.stepId}")
                    println("  📝 原因: ${event.reason}")
                }
                is ErrorOccurredEvent -> {
                    println("  🔴 错误发生: ${event.data["errorClass"]}")
                    println("  📝 消息: ${event.data["errorMessage"]}")
                }
                else -> {
                    println("  📝 其他事件数据: ${event.data}")
                }
            }
        }
        
        // 只监听特定类型的事件
        override fun getSupportedEventTypes(): List<WorkflowEventType>? {
            return listOf(
                WorkflowEventType.WORKFLOW_STARTED,
                WorkflowEventType.WORKFLOW_COMPLETED,
                WorkflowEventType.WORKFLOW_FAILED,
                WorkflowEventType.STEP_STARTED,
                WorkflowEventType.STEP_COMPLETED,
                WorkflowEventType.STEP_FAILED,
                WorkflowEventType.STEP_SKIPPED,
                WorkflowEventType.ERROR_OCCURRED
            )
        }
    }
    
    // 注册事件监听器
    eventBus.registerListener(eventListener)
    
    // 创建工作流回调
    val workflowCallback = object : WorkflowCallback {
        override suspend fun beforeWorkflowStart(workflowId: String, runId: String, input: Map<String, Any?>) {
            println("🔔 工作流开始前回调: $workflowId, 运行ID: $runId")
            println("📥 输入: $input")
        }
        
        override suspend fun afterWorkflowComplete(workflowId: String, runId: String, output: Map<String, Any?>, executionTime: Long) {
            println("🔔 工作流完成后回调: $workflowId, 运行ID: $runId")
            println("📤 输出: $output")
            println("⏱️ 执行时间: ${executionTime}ms")
        }
        
        override suspend fun onWorkflowFail(workflowId: String, runId: String, error: String?, executionTime: Long) {
            println("🔔 工作流失败回调: $workflowId, 运行ID: $runId")
            println("🔴 错误: $error")
            println("⏱️ 执行时间: ${executionTime}ms")
        }
    }
    
    // 创建步骤回调
    val stepCallback = object : StepCallback {
        override suspend fun beforeStepExecute(workflowId: String, runId: String, stepId: String, context: WorkflowContext): Boolean {
            println("🔔 步骤执行前回调: $stepId")
            
            // 如果步骤ID为"skipMe"，则跳过该步骤
            if (stepId == "skipMe") {
                println("⏭️ 跳过步骤: $stepId")
                return false
            }
            
            return true
        }
        
        override suspend fun afterStepExecute(
            workflowId: String,
            runId: String,
            stepId: String,
            result: WorkflowStepResult,
            context: WorkflowContext
        ): WorkflowStepResult {
            println("🔔 步骤执行后回调: $stepId")
            println("📤 输出: ${result.output}")
            
            // 如果步骤ID为"modifyMe"，则修改输出
            if (stepId == "modifyMe") {
                println("🔄 修改步骤输出: $stepId")
                val modifiedOutput = result.output.toMutableMap()
                modifiedOutput["modified"] = true
                modifiedOutput["timestamp"] = System.currentTimeMillis()
                
                return WorkflowStepResult(
                    stepId = result.stepId,
                    success = result.success,
                    output = modifiedOutput,
                    error = result.error,
                    executionTime = result.executionTime
                )
            }
            
            return result
        }
        
        override suspend fun onStepFail(
            workflowId: String,
            runId: String,
            stepId: String,
            error: String?,
            exception: Throwable?,
            context: WorkflowContext
        ): Boolean {
            println("🔔 步骤失败回调: $stepId")
            println("🔴 错误: $error")
            
            // 如果步骤ID为"recoverMe"，则处理错误
            if (stepId == "recoverMe") {
                println("🔄 恢复步骤: $stepId")
                return true // 返回true表示已处理错误
            }
            
            return false // 返回false表示未处理错误
        }
    }
    
    // 创建步骤
    val step1 = object : WorkflowStep {
        override val id: String = "step1"
        override val name: String = "Step 1"
        override val description: String = "First step"
        override val after: List<String> = emptyList()
        override val variables: Map<String, VariableReference> = emptyMap()
        
        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行步骤1...")
            delay(500) // 模拟工作
            
            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf("message" to "步骤1完成", "timestamp" to System.currentTimeMillis()),
                executionTime = 500
            )
        }
    }
    
    val step2 = object : WorkflowStep {
        override val id: String = "modifyMe"
        override val name: String = "Step 2 (Modify Me)"
        override val description: String = "Second step - will be modified by callback"
        override val after: List<String> = listOf("step1")
        override val variables: Map<String, VariableReference> = emptyMap()
        
        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行步骤2...")
            delay(300) // 模拟工作
            
            val step1Output = context.steps["step1"]?.output ?: emptyMap()
            
            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf(
                    "message" to "步骤2完成",
                    "previousStep" to step1Output["message"],
                    "timestamp" to System.currentTimeMillis()
                ),
                executionTime = 300
            )
        }
    }
    
    val step3 = object : WorkflowStep {
        override val id: String = "skipMe"
        override val name: String = "Step 3 (Skip Me)"
        override val description: String = "Third step - will be skipped by callback"
        override val after: List<String> = listOf("modifyMe")
        override val variables: Map<String, VariableReference> = emptyMap()
        
        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行步骤3...")
            delay(200) // 模拟工作
            
            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf("message" to "步骤3完成", "timestamp" to System.currentTimeMillis()),
                executionTime = 200
            )
        }
    }
    
    val step4 = object : WorkflowStep {
        override val id: String = "recoverMe"
        override val name: String = "Step 4 (Recover Me)"
        override val description: String = "Fourth step - will fail but be recovered by callback"
        override val after: List<String> = listOf("skipMe")
        override val variables: Map<String, VariableReference> = emptyMap()
        
        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行步骤4...")
            delay(100) // 模拟工作
            
            // 故意抛出异常
            if (true) {
                throw RuntimeException("步骤4故意失败")
            }
            
            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf("message" to "步骤4完成", "timestamp" to System.currentTimeMillis()),
                executionTime = 100
            )
        }
    }
    
    val finalStep = object : WorkflowStep {
        override val id: String = "final"
        override val name: String = "Final Step"
        override val description: String = "Final step"
        override val after: List<String> = listOf("recoverMe")
        override val variables: Map<String, VariableReference> = emptyMap()
        
        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行最终步骤...")
            delay(400) // 模拟工作
            
            // 收集所有步骤的输出
            val outputs = context.steps.mapValues { (_, result) -> result.output["message"] }
            
            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf(
                    "message" to "最终步骤完成",
                    "allSteps" to outputs,
                    "timestamp" to System.currentTimeMillis()
                ),
                executionTime = 400
            )
        }
    }
    
    // 创建工作流
    val eventWorkflow = workflow("EventWorkflow", "事件和回调示例工作流") {
        step(step1)
        step(step2)
        step(step3)
        step(step4)
        step(finalStep)
    }
    
    // 创建事件感知工作流引擎
    val workflowEngine = EventAwareWorkflowEngine(
        workflows = mapOf("EventWorkflow" to eventWorkflow),
        stateStorage = stateStorage,
        eventBus = eventBus,
        eventStorage = eventStorage
    )
    
    // 注册工作流回调
    workflowEngine.registerWorkflowCallback(workflowCallback)
    
    // 注册步骤回调
    workflowEngine.registerStepCallback(stepCallback)
    
    // 启动事件流收集器
    val eventCollectorJob = launch {
        eventBus.getEventFlow()
            .collect { event ->
                // 事件已经由监听器处理，这里不需要额外处理
            }
    }
    
    // 执行工作流
    println("\n开始执行工作流...")
    val result = workflowEngine.executeWorkflow(
        workflowId = "EventWorkflow",
        input = mapOf("startTime" to System.currentTimeMillis())
    )
    
    // 检查工作流执行结果
    if (result.success) {
        println("\n工作流执行成功!")
        println("执行时间: ${result.executionTime}ms")
        println("步骤数: ${result.steps.size}")
        println("输出: ${result.output}")
    } else {
        println("\n工作流执行失败!")
        println("错误: ${result.error}")
    }
    
    // 查询并显示事件
    println("\n工作流事件历史:")
    val events = eventStorage.getEvents("EventWorkflow", result.runId ?: "")
    events.sortedBy { it.timestamp }.forEachIndexed { index, event ->
        val timestamp = Instant.ofEpochMilli(event.timestamp)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        
        println("${index + 1}. [${timestamp}] ${event.type} - ${event.stepId ?: "工作流级别"}")
    }
    
    // 取消事件收集器
    eventCollectorJob.cancel()
    
    println("\n示例完成!")
}
