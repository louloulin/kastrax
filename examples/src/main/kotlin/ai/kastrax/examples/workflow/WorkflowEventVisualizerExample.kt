package ai.kastrax.examples.workflow

import ai.kastrax.core.agent.agent
import ai.kastrax.core.workflow.*
import ai.kastrax.core.workflow.builder.workflow
import ai.kastrax.core.workflow.engine.EventAwareWorkflowEngine
import ai.kastrax.core.workflow.event.DefaultWorkflowEventBus
import ai.kastrax.core.workflow.event.InMemoryWorkflowEventStorage
import ai.kastrax.core.workflow.state.InMemoryWorkflowStateStorage
import ai.kastrax.integrations.openai.openAi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.UUID

/**
 * 工作流事件可视化器示例，展示如何使用事件可视化器。
 */
fun main() = runBlocking {
    println("开始工作流事件可视化器示例...")
    
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
    
    // 创建事件可视化器
    val visualizer = WorkflowEventVisualizer(eventStorage)
    
    // 创建步骤
    val step1 = object : WorkflowStep {
        override val id: String = "step1"
        override val name: String = "数据收集"
        override val description: String = "收集输入数据"
        override val after: List<String> = emptyList()
        override val variables: Map<String, VariableReference> = emptyMap()
        
        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行数据收集步骤...")
            delay(500) // 模拟工作
            
            val inputValue = context.input["value"] as? Int ?: 0
            
            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf(
                    "value" to inputValue,
                    "timestamp" to System.currentTimeMillis()
                ),
                executionTime = 500
            )
        }
    }
    
    val step2 = object : WorkflowStep {
        override val id: String = "step2"
        override val name: String = "数据处理"
        override val description: String = "处理收集的数据"
        override val after: List<String> = listOf("step1")
        override val variables: Map<String, VariableReference> = emptyMap()
        
        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行数据处理步骤...")
            delay(800) // 模拟工作
            
            val value = context.steps["step1"]?.output?.get("value") as? Int ?: 0
            val processedValue = value * 2
            
            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf(
                    "value" to processedValue,
                    "originalValue" to value,
                    "timestamp" to System.currentTimeMillis()
                ),
                executionTime = 800
            )
        }
    }
    
    val step3 = object : WorkflowStep {
        override val id: String = "step3"
        override val name: String = "数据验证"
        override val description: String = "验证处理后的数据"
        override val after: List<String> = listOf("step2")
        override val variables: Map<String, VariableReference> = emptyMap()
        
        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行数据验证步骤...")
            delay(300) // 模拟工作
            
            val value = context.steps["step2"]?.output?.get("value") as? Int ?: 0
            
            // 如果值小于50，则验证失败
            if (value < 50) {
                return WorkflowStepResult.failed(
                    stepId = id,
                    error = Exception("验证失败: 值 $value 小于50")
                )
            }
            
            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf(
                    "value" to value,
                    "valid" to true,
                    "timestamp" to System.currentTimeMillis()
                ),
                executionTime = 300
            )
        }
    }
    
    val step4 = object : WorkflowStep {
        override val id: String = "step4"
        override val name: String = "数据存储"
        override val description: String = "存储验证后的数据"
        override val after: List<String> = listOf("step3")
        override val variables: Map<String, VariableReference> = emptyMap()
        
        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行数据存储步骤...")
            delay(600) // 模拟工作
            
            val value = context.steps["step3"]?.output?.get("value") as? Int ?: 0
            
            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf(
                    "value" to value,
                    "stored" to true,
                    "location" to "database",
                    "timestamp" to System.currentTimeMillis()
                ),
                executionTime = 600
            )
        }
    }
    
    val finalStep = object : WorkflowStep {
        override val id: String = "finalStep"
        override val name: String = "结果汇总"
        override val description: String = "汇总处理结果"
        override val after: List<String> = listOf("step4")
        override val variables: Map<String, VariableReference> = emptyMap()
        
        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行结果汇总步骤...")
            delay(400) // 模拟工作
            
            // 收集所有步骤的输出
            val step1Output = context.steps["step1"]?.output ?: emptyMap()
            val step2Output = context.steps["step2"]?.output ?: emptyMap()
            val step3Output = context.steps["step3"]?.output ?: emptyMap()
            val step4Output = context.steps["step4"]?.output ?: emptyMap()
            
            // 创建汇总结果
            val summary = mapOf(
                "originalValue" to step1Output["value"],
                "processedValue" to step2Output["value"],
                "valid" to step3Output["valid"],
                "stored" to step4Output["stored"],
                "location" to step4Output["location"],
                "timestamp" to System.currentTimeMillis()
            )
            
            return WorkflowStepResult.success(
                stepId = id,
                output = mapOf(
                    "summary" to summary,
                    "timestamp" to System.currentTimeMillis()
                ),
                executionTime = 400
            )
        }
    }
    
    // 创建工作流
    val visualizerWorkflow = workflow("VisualizerWorkflow", "事件可视化器示例工作流") {
        step(step1)
        step(step2)
        step(step3)
        step(step4)
        step(finalStep)
    }
    
    // 创建事件感知工作流引擎
    val workflowEngine = EventAwareWorkflowEngine(
        workflows = mapOf("VisualizerWorkflow" to visualizerWorkflow),
        stateStorage = stateStorage,
        eventBus = eventBus,
        eventStorage = eventStorage
    )
    
    // 执行成功的工作流
    println("\n执行成功的工作流...")
    val successRunId = UUID.randomUUID().toString()
    val successResult = workflowEngine.executeWorkflow(
        workflowId = "VisualizerWorkflow",
        input = mapOf("value" to 30) // 值为30，会被处理为60，通过验证
    )
    
    // 检查工作流执行结果
    if (successResult.success) {
        println("\n工作流执行成功!")
        println("执行时间: ${successResult.executionTime}ms")
        println("步骤数: ${successResult.steps.size}")
    } else {
        println("\n工作流执行失败!")
        println("错误: ${successResult.error}")
    }
    
    // 执行失败的工作流
    println("\n执行失败的工作流...")
    val failureRunId = UUID.randomUUID().toString()
    val failureResult = workflowEngine.executeWorkflow(
        workflowId = "VisualizerWorkflow",
        input = mapOf("value" to 10) // 值为10，会被处理为20，不通过验证
    )
    
    // 检查工作流执行结果
    if (failureResult.success) {
        println("\n工作流执行成功!")
        println("执行时间: ${failureResult.executionTime}ms")
        println("步骤数: ${failureResult.steps.size}")
    } else {
        println("\n工作流执行失败!")
        println("错误: ${failureResult.error}")
    }
    
    // 显示成功工作流的可视化
    println("\n成功工作流的时间线:")
    println(visualizer.generateTimeline("VisualizerWorkflow", successResult.runId ?: ""))
    
    println("\n成功工作流的步骤图:")
    println(visualizer.generateStepGraph("VisualizerWorkflow", successResult.runId ?: ""))
    
    println("\n成功工作流的统计信息:")
    println(visualizer.generateStats("VisualizerWorkflow", successResult.runId ?: ""))
    
    // 显示失败工作流的可视化
    println("\n失败工作流的时间线:")
    println(visualizer.generateTimeline("VisualizerWorkflow", failureResult.runId ?: ""))
    
    println("\n失败工作流的步骤图:")
    println(visualizer.generateStepGraph("VisualizerWorkflow", failureResult.runId ?: ""))
    
    println("\n失败工作流的统计信息:")
    println(visualizer.generateStats("VisualizerWorkflow", failureResult.runId ?: ""))
    
    println("\n示例完成!")
}
