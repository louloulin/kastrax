package ai.kastrax.examples.workflow

import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.builder.workflow
import ai.kastrax.core.workflow.engine.WorkflowEngine
import ai.kastrax.core.workflow.state.InMemoryWorkflowStateStorage
import ai.kastrax.core.workflow.suspend.AbstractSuspendableStep
import ai.kastrax.core.workflow.suspend.SuspendController
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 工作流引擎测试。
 */
class WorkflowEngineTest {
    
    /**
     * 测试基本工作流执行。
     */
    @Test
    fun testBasicWorkflowExecution() = runBlocking {
        // 创建步骤
        val step1 = object : WorkflowStep {
            override val id: String = "step1"
            override val name: String = "Step 1"
            override val description: String = "First step"
            override val after: List<String> = emptyList()
            
            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                return WorkflowStepResult.success(
                    stepId = id,
                    output = mapOf("result" to JsonPrimitive("Step 1 result"))
                )
            }
        }
        
        val step2 = object : WorkflowStep {
            override val id: String = "step2"
            override val name: String = "Step 2"
            override val description: String = "Second step"
            override val after: List<String> = listOf("step1")
            
            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                val step1Result = context.getStepOutput("step1")?.get("result")?.toString()?.removeSurrounding("\"")
                    ?: "No result"
                
                return WorkflowStepResult.success(
                    stepId = id,
                    output = mapOf("result" to JsonPrimitive("$step1Result -> Step 2 result"))
                )
            }
        }
        
        // 创建工作流
        val testWorkflow = workflow("TestWorkflow", "Test workflow") {
            step(step1)
            step(step2)
        }
        
        // 创建工作流引擎
        val workflowEngine = WorkflowEngine(
            workflows = mapOf("TestWorkflow" to testWorkflow),
            stateStorage = InMemoryWorkflowStateStorage()
        )
        
        // 执行工作流
        val result = workflowEngine.executeWorkflow(
            workflowId = "TestWorkflow",
            input = emptyMap()
        )
        
        // 验证结果
        assertTrue(result.success)
        assertEquals(2, result.steps.size)
        assertTrue(result.steps.containsKey("step1"))
        assertTrue(result.steps.containsKey("step2"))
        
        val step1Result = result.steps["step1"]
        assertNotNull(step1Result)
        assertTrue(step1Result!!.success)
        assertEquals("Step 1 result", step1Result.output["result"]?.toString()?.removeSurrounding("\""))
        
        val step2Result = result.steps["step2"]
        assertNotNull(step2Result)
        assertTrue(step2Result!!.success)
        assertEquals("Step 1 result -> Step 2 result", step2Result.output["result"]?.toString()?.removeSurrounding("\""))
    }
    
    /**
     * 测试条件分支。
     */
    @Test
    fun testConditionalBranch() = runBlocking {
        // 创建工作流
        val conditionalWorkflow = workflow("ConditionalWorkflow", "Conditional workflow") {
            // 初始步骤
            step(object : WorkflowStep {
                override val id: String = "init"
                override val name: String = "Initial Step"
                override val description: String = "Initial step"
                override val after: List<String> = emptyList()
                
                override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                    val value = context.input["value"]?.toString()?.toIntOrNull() ?: 0
                    
                    return WorkflowStepResult.success(
                        stepId = id,
                        output = mapOf("value" to JsonPrimitive(value))
                    )
                }
            })
            
            // 条件分支
            ifThen(
                condition = { context ->
                    val value = context.getStepOutput("init")?.get("value")?.toString()?.toIntOrNull() ?: 0
                    value > 10
                },
                thenStep = object : WorkflowStep {
                    override val id: String = "highValue"
                    override val name: String = "High Value"
                    override val description: String = "High value branch"
                    override val after: List<String> = emptyList()
                    
                    override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                        val value = context.getStepOutput("init")?.get("value")?.toString()?.toIntOrNull() ?: 0
                        
                        return WorkflowStepResult.success(
                            stepId = id,
                            output = mapOf(
                                "message" to JsonPrimitive("High value: $value"),
                                "category" to JsonPrimitive("high")
                            )
                        )
                    }
                },
                elseStep = object : WorkflowStep {
                    override val id: String = "lowValue"
                    override val name: String = "Low Value"
                    override val description: String = "Low value branch"
                    override val after: List<String> = emptyList()
                    
                    override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                        val value = context.getStepOutput("init")?.get("value")?.toString()?.toIntOrNull() ?: 0
                        
                        return WorkflowStepResult.success(
                            stepId = id,
                            output = mapOf(
                                "message" to JsonPrimitive("Low value: $value"),
                                "category" to JsonPrimitive("low")
                            )
                        )
                    }
                }
            )
        }
        
        // 创建工作流引擎
        val workflowEngine = WorkflowEngine(
            workflows = mapOf("ConditionalWorkflow" to conditionalWorkflow),
            stateStorage = InMemoryWorkflowStateStorage()
        )
        
        // 测试高值分支
        val highValueResult = workflowEngine.executeWorkflow(
            workflowId = "ConditionalWorkflow",
            input = mapOf("value" to JsonPrimitive(20))
        )
        
        assertTrue(highValueResult.success)
        val conditionalStepId = highValueResult.steps.keys.find { it.startsWith("if-") }
        assertNotNull(conditionalStepId)
        
        val conditionalResult = highValueResult.steps[conditionalStepId]
        assertNotNull(conditionalResult)
        assertEquals("then", conditionalResult!!.output["branch"]?.toString()?.removeSurrounding("\""))
        
        // 测试低值分支
        val lowValueResult = workflowEngine.executeWorkflow(
            workflowId = "ConditionalWorkflow",
            input = mapOf("value" to JsonPrimitive(5))
        )
        
        assertTrue(lowValueResult.success)
        val conditionalStepId2 = lowValueResult.steps.keys.find { it.startsWith("if-") }
        assertNotNull(conditionalStepId2)
        
        val conditionalResult2 = lowValueResult.steps[conditionalStepId2]
        assertNotNull(conditionalResult2)
        assertEquals("else", conditionalResult2!!.output["branch"]?.toString()?.removeSurrounding("\""))
    }
    
    /**
     * 测试并行执行。
     */
    @Test
    fun testParallelExecution() = runBlocking {
        // 创建工作流
        val parallelWorkflow = workflow("ParallelWorkflow", "Parallel workflow") {
            // 初始步骤
            step(object : WorkflowStep {
                override val id: String = "init"
                override val name: String = "Initial Step"
                override val description: String = "Initial step"
                override val after: List<String> = emptyList()
                
                override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                    return WorkflowStepResult.success(
                        stepId = id,
                        output = mapOf("value" to JsonPrimitive("Initial value"))
                    )
                }
            })
            
            // 并行步骤
            parallel(
                object : WorkflowStep {
                    override val id: String = "branch1"
                    override val name: String = "Branch 1"
                    override val description: String = "First parallel branch"
                    override val after: List<String> = emptyList()
                    
                    override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                        return WorkflowStepResult.success(
                            stepId = id,
                            output = mapOf("result" to JsonPrimitive("Branch 1 result"))
                        )
                    }
                },
                object : WorkflowStep {
                    override val id: String = "branch2"
                    override val name: String = "Branch 2"
                    override val description: String = "Second parallel branch"
                    override val after: List<String> = emptyList()
                    
                    override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                        return WorkflowStepResult.success(
                            stepId = id,
                            output = mapOf("result" to JsonPrimitive("Branch 2 result"))
                        )
                    }
                }
            )
        }
        
        // 创建工作流引擎
        val workflowEngine = WorkflowEngine(
            workflows = mapOf("ParallelWorkflow" to parallelWorkflow),
            stateStorage = InMemoryWorkflowStateStorage()
        )
        
        // 执行工作流
        val result = workflowEngine.executeWorkflow(
            workflowId = "ParallelWorkflow",
            input = emptyMap()
        )
        
        // 验证结果
        assertTrue(result.success)
        
        // 找到并行步骤
        val parallelStepId = result.steps.keys.find { it.startsWith("parallel-") }
        assertNotNull(parallelStepId)
        
        val parallelResult = result.steps[parallelStepId]
        assertNotNull(parallelResult)
        assertTrue(parallelResult!!.success)
        
        // 验证并行分支结果
        val results = parallelResult.output["results"]
        assertNotNull(results)
        
        val branch1Result = results?.get("branch1")?.get("result")?.toString()?.removeSurrounding("\"")
        assertEquals("Branch 1 result", branch1Result)
        
        val branch2Result = results?.get("branch2")?.get("result")?.toString()?.removeSurrounding("\"")
        assertEquals("Branch 2 result", branch2Result)
    }
    
    /**
     * 测试循环执行。
     */
    @Test
    fun testLoopExecution() = runBlocking {
        // 创建工作流
        val loopWorkflow = workflow("LoopWorkflow", "Loop workflow") {
            // 初始步骤
            step(object : WorkflowStep {
                override val id: String = "init"
                override val name: String = "Initial Step"
                override val description: String = "Initial step"
                override val after: List<String> = emptyList()
                
                override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                    return WorkflowStepResult.success(
                        stepId = id,
                        output = mapOf("count" to JsonPrimitive(0))
                    )
                }
            })
            
            // 循环步骤
            loop(
                condition = { context, iteration ->
                    val count = context.getStepOutput("init")?.get("count")?.toString()?.toIntOrNull() ?: 0
                    iteration < 3 // 执行3次循环
                },
                body = object : WorkflowStep {
                    override val id: String = "loopBody"
                    override val name: String = "Loop Body"
                    override val description: String = "Loop body step"
                    override val after: List<String> = emptyList()
                    
                    override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                        val iteration = context.variables["iteration"] as? Int ?: 0
                        
                        return WorkflowStepResult.success(
                            stepId = id,
                            output = mapOf(
                                "iteration" to JsonPrimitive(iteration),
                                "message" to JsonPrimitive("Iteration $iteration")
                            )
                        )
                    }
                }
            )
        }
        
        // 创建工作流引擎
        val workflowEngine = WorkflowEngine(
            workflows = mapOf("LoopWorkflow" to loopWorkflow),
            stateStorage = InMemoryWorkflowStateStorage()
        )
        
        // 执行工作流
        val result = workflowEngine.executeWorkflow(
            workflowId = "LoopWorkflow",
            input = emptyMap()
        )
        
        // 验证结果
        assertTrue(result.success)
        
        // 找到循环步骤
        val loopStepId = result.steps.keys.find { it.startsWith("loop-") }
        assertNotNull(loopStepId)
        
        val loopResult = result.steps[loopStepId]
        assertNotNull(loopResult)
        assertTrue(loopResult!!.success)
        
        // 验证循环次数
        val iterations = loopResult.output["iterations"]?.toString()?.toIntOrNull()
        assertEquals(3, iterations)
        
        // 验证循环结果
        val results = loopResult.output["results"]
        assertNotNull(results)
    }
    
    /**
     * 测试工作流暂停和恢复。
     */
    @Test
    fun testWorkflowSuspendAndResume() = runBlocking {
        // 创建可暂停步骤
        val suspendableStep = object : AbstractSuspendableStep(
            id = "suspendable",
            name = "Suspendable Step",
            description = "Step that can be suspended"
        ) {
            override suspend fun execute(
                context: WorkflowContext,
                suspendController: SuspendController
            ): WorkflowStepResult {
                // 检查是否是恢复执行
                if (suspendController.isResumed()) {
                    val resumeData = suspendController.getResumeData()
                    val message = resumeData?.get("message")?.toString()?.removeSurrounding("\"") ?: "No message"
                    
                    return WorkflowStepResult.success(
                        stepId = id,
                        output = mapOf(
                            "message" to JsonPrimitive("Resumed with: $message"),
                            "resumed" to JsonPrimitive(true)
                        )
                    )
                }
                
                // 暂停工作流
                suspendController.suspend(buildJsonObject {
                    put("originalMessage", JsonPrimitive("Please provide input"))
                })
                
                // 这里的代码不会执行
                return WorkflowStepResult.success(id, emptyMap())
            }
        }
        
        // 创建工作流
        val suspendWorkflow = workflow("SuspendWorkflow", "Suspend workflow") {
            suspendableStep(suspendableStep)
        }
        
        // 创建工作流引擎
        val workflowEngine = WorkflowEngine(
            workflows = mapOf("SuspendWorkflow" to suspendWorkflow),
            stateStorage = InMemoryWorkflowStateStorage()
        )
        
        // 执行工作流
        val result = workflowEngine.executeWorkflow(
            workflowId = "SuspendWorkflow",
            input = emptyMap()
        )
        
        // 验证工作流已暂停
        val suspendedStep = result.steps["suspendable"]
        assertNotNull(suspendedStep)
        assertEquals(ai.kastrax.core.workflow.StepStatus.SUSPENDED, suspendedStep!!.status)
        
        // 验证暂停数据
        val suspendPayload = suspendedStep.suspendPayload
        assertNotNull(suspendPayload)
        assertEquals("Please provide input", suspendPayload!!["originalMessage"]?.toString()?.removeSurrounding("\""))
        
        // 恢复工作流
        val resumeData = buildJsonObject {
            put("message", JsonPrimitive("Human input"))
        }
        
        val resumeResult = workflowEngine.resumeWorkflow(
            executionId = result.executionId,
            stepId = "suspendable",
            data = resumeData
        )
        
        // 验证恢复结果
        assertTrue(resumeResult.success)
        
        val resumedStep = resumeResult.steps["suspendable"]
        assertNotNull(resumedStep)
        assertTrue(resumedStep!!.success)
        assertEquals(ai.kastrax.core.workflow.StepStatus.COMPLETED, resumedStep.status)
        
        // 验证恢复后的输出
        val message = resumedStep.output["message"]?.toString()?.removeSurrounding("\"")
        assertEquals("Resumed with: Human input", message)
        
        val resumed = resumedStep.output["resumed"]?.toString()?.toBoolean()
        assertTrue(resumed!!)
    }
}
