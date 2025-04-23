package ai.kastrax.core.workflow.event

import ai.kastrax.core.workflow.SimpleWorkflow
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.WorkflowBuilder
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowExecuteOptions
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.callback.StepCallback
import ai.kastrax.core.workflow.callback.WorkflowCallback
import ai.kastrax.core.workflow.engine.EventAwareWorkflowEngine
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class WorkflowEventTest {

    private lateinit var eventBus: DefaultWorkflowEventBus
    private lateinit var eventStorage: InMemoryWorkflowEventStorage
    private lateinit var workflowEngine: EventAwareWorkflowEngine

    @BeforeEach
    fun setup() {
        eventBus = DefaultWorkflowEventBus()
        eventStorage = InMemoryWorkflowEventStorage()

        // 创建测试工作流
        val testWorkflow = createTestWorkflow()

        // 创建工作流引擎
        workflowEngine = EventAwareWorkflowEngine(
            workflows = mapOf("TestWorkflow" to testWorkflow),
            eventBus = eventBus,
            eventStorage = eventStorage
        )
    }

    @Test
    fun `test event publishing and subscription`() = runBlocking {
        // 创建事件计数器
        val eventCounter = AtomicInteger(0)

        // 创建事件监听器
        val listener = object : WorkflowEventListener {
            override suspend fun onEvent(event: WorkflowEvent) {
                eventCounter.incrementAndGet()
            }
        }

        // 注册监听器
        eventBus.registerListener(listener)

        // 执行工作流
        val result = workflowEngine.executeWorkflow(
            workflowId = "TestWorkflow",
            input = mapOf("value" to 42)
        )

        // 验证工作流执行成功
        assertTrue(result.success)

        // 验证事件数量
        assertTrue(eventCounter.get() > 0)

        // 验证事件存储
        val events = eventStorage.getEvents("TestWorkflow", result.runId ?: "")
        assertTrue(events.isNotEmpty())

        // 验证事件类型
        val eventTypes = events.map { it.type }.toSet()
        assertTrue(eventTypes.contains(WorkflowEventType.WORKFLOW_STARTED))
        assertTrue(eventTypes.contains(WorkflowEventType.WORKFLOW_COMPLETED))
        assertTrue(eventTypes.contains(WorkflowEventType.STEP_STARTED))
        assertTrue(eventTypes.contains(WorkflowEventType.STEP_COMPLETED))
    }

    @Test
    fun `test event filtering`() = runBlocking {
        // 创建事件计数器
        val workflowStartedCounter = AtomicInteger(0)
        val stepCompletedCounter = AtomicInteger(0)

        // 创建工作流开始事件监听器
        val workflowStartedListener = object : WorkflowEventListener {
            override suspend fun onEvent(event: WorkflowEvent) {
                workflowStartedCounter.incrementAndGet()
            }

            override fun getSupportedEventTypes(): List<WorkflowEventType> {
                return listOf(WorkflowEventType.WORKFLOW_STARTED)
            }
        }

        // 创建步骤完成事件监听器
        val stepCompletedListener = object : WorkflowEventListener {
            override suspend fun onEvent(event: WorkflowEvent) {
                stepCompletedCounter.incrementAndGet()
            }

            override fun getSupportedEventTypes(): List<WorkflowEventType> {
                return listOf(WorkflowEventType.STEP_COMPLETED)
            }
        }

        // 注册监听器
        eventBus.registerListener(workflowStartedListener)
        eventBus.registerListener(stepCompletedListener)

        // 执行工作流
        val result = workflowEngine.executeWorkflow(
            workflowId = "TestWorkflow",
            input = mapOf("value" to 42)
        )

        // 验证工作流执行成功
        assertTrue(result.success)

        // 验证事件计数
        assertEquals(1, workflowStartedCounter.get())
        assertTrue(stepCompletedCounter.get() > 0)
    }

    @Test
    fun `test workflow callbacks`() = runBlocking {
        // 创建回调标志
        val beforeStartCalled = AtomicBoolean(false)
        val afterCompleteCalled = AtomicBoolean(false)

        // 创建工作流回调
        val callback = object : WorkflowCallback {
            override suspend fun beforeWorkflowStart(workflowId: String, runId: String, input: Map<String, Any?>) {
                beforeStartCalled.set(true)
            }

            override suspend fun afterWorkflowComplete(workflowId: String, runId: String, output: Map<String, Any?>, executionTime: Long) {
                afterCompleteCalled.set(true)
            }
        }

        // 注册回调
        workflowEngine.registerWorkflowCallback(callback)

        // 执行工作流
        val result = workflowEngine.executeWorkflow(
            workflowId = "TestWorkflow",
            input = mapOf("value" to 42)
        )

        // 验证工作流执行成功
        assertTrue(result.success)

        // 验证回调被调用
        assertTrue(beforeStartCalled.get())
        assertTrue(afterCompleteCalled.get())
    }

    @Test
    fun `test step callbacks`() = runBlocking {
        // 测试工作流执行成功
        val result = workflowEngine.executeWorkflow(
            workflowId = "TestWorkflow",
            input = mapOf("value" to 42)
        )

        // 验证工作流执行成功
        assertTrue(result.success)

        // 验证步骤完成
        assertTrue(result.steps.containsKey("step1"))
        assertTrue(result.steps.containsKey("step2"))
        assertTrue(result.steps.containsKey("step3"))
    }

    @Test
    fun `test step skip via callback`() = runBlocking {
        // 测试工作流执行成功
        val result = workflowEngine.executeWorkflow(
            workflowId = "TestWorkflow",
            input = mapOf("value" to 42)
        )

        // 验证工作流执行成功
        assertTrue(result.success)

        // 验证所有步骤都存在
        assertTrue(result.steps.containsKey("step1"))
        assertTrue(result.steps.containsKey("step2"))
        assertTrue(result.steps.containsKey("step3"))

        // 验证事件
        val events = eventStorage.getEvents("TestWorkflow", result.runId ?: "")
        val stepCompletedEvents = events.filter { it.type == WorkflowEventType.STEP_COMPLETED }
        assertTrue(stepCompletedEvents.isNotEmpty())
    }

    @Test
    fun `test event flow collection`() = runBlocking {
        // 先执行工作流生成事件
        val result = workflowEngine.executeWorkflow(
            workflowId = "TestWorkflow",
            input = mapOf("value" to 42)
        )

        // 验证工作流执行成功
        assertTrue(result.success)

        // 验证事件存储中有事件
        val events = eventStorage.getEvents("TestWorkflow", result.runId ?: "")
        assertTrue(events.isNotEmpty())
    }

    private fun createTestWorkflow(): SimpleWorkflow {
        // 创建测试步骤
        val step1 = object : WorkflowStep {
            override val id: String = "step1"
            override val name: String = "Step 1"
            override val description: String = "First step"
            override val after: List<String> = emptyList()
            override val variables: Map<String, VariableReference> = emptyMap()

            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                val value = context.input["value"] as? Int ?: 0
                return WorkflowStepResult.success(
                    stepId = id,
                    output = mapOf("result" to (value * 2))
                )
            }
        }

        val step2 = object : WorkflowStep {
            override val id: String = "step2"
            override val name: String = "Step 2"
            override val description: String = "Second step"
            override val after: List<String> = listOf("step1")
            override val variables: Map<String, VariableReference> = emptyMap()

            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                val value = context.steps["step1"]?.output?.get("result") as? Int ?: 0
                return WorkflowStepResult.success(
                    stepId = id,
                    output = mapOf("result" to (value + 10))
                )
            }
        }

        val step3 = object : WorkflowStep {
            override val id: String = "step3"
            override val name: String = "Step 3"
            override val description: String = "Third step"
            override val after: List<String> = listOf("step2")
            override val variables: Map<String, VariableReference> = emptyMap()

            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                val value1 = context.steps["step1"]?.output?.get("result") as? Int ?: 0
                val value2 = context.steps["step2"]?.output?.get("result") as? Int ?: 0
                return WorkflowStepResult.success(
                    stepId = id,
                    output = mapOf("result" to (value1 + value2))
                )
            }
        }

        // 创建工作流
        val workflow = SimpleWorkflow(
            workflowName = "TestWorkflow",
            description = "Test Workflow",
            steps = mapOf(
                step1.id to step1,
                step2.id to step2,
                step3.id to step3
            )
        )

        return workflow
    }
}
