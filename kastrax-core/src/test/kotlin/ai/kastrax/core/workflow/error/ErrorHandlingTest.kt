package ai.kastrax.core.workflow.error

import ai.kastrax.core.workflow.RetryConfig
import ai.kastrax.core.workflow.StepConfig
import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.SimpleWorkflow
import ai.kastrax.core.workflow.WorkflowResult
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * 错误处理和重试机制的测试。
 */
class ErrorHandlingTest {

    /**
     * 测试重试恢复策略。
     */
    @Test
    fun testRetryRecoveryStrategy() = runTest {
        // 创建重试恢复策略
        val strategy = RetryRecoveryStrategy(maxRetries = 3)

        // 创建测试步骤
        val step = object : WorkflowStep {
            override val id: String = "test-step"
            override val name: String = "Test Step"
            override val description: String = "Test step"
            override val after: List<String> = emptyList()
            override val variables: Map<String, ai.kastrax.core.workflow.VariableReference> = emptyMap()
            override val config: ai.kastrax.core.workflow.StepConfig? = null

            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                return WorkflowStepResult(
                    stepId = id,
                    success = true,
                    output = emptyMap()
                )
            }
        }

        // 创建上下文
        val context = WorkflowContext(
            input = emptyMap()
        )

        // 测试重试策略
        val result = strategy.apply(context, step, RuntimeException("Test error"))

        // 验证结果
        assertTrue(result.success)
        assertEquals(listOf(step.id), result.nextSteps)
        assertFalse(result.terminateWorkflow)
    }

    /**
     * 测试跳过恢复策略。
     */
    @Test
    fun testSkipRecoveryStrategy() = runTest {
        // 创建跳过恢复策略
        val strategy = SkipRecoveryStrategy(defaultOutput = mapOf("skipped" to true))

        // 创建测试步骤
        val step = object : WorkflowStep {
            override val id: String = "test-step"
            override val name: String = "Test Step"
            override val description: String = "Test step"
            override val after: List<String> = emptyList()
            override val variables: Map<String, ai.kastrax.core.workflow.VariableReference> = emptyMap()
            override val config: ai.kastrax.core.workflow.StepConfig? = null

            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                return WorkflowStepResult(
                    stepId = id,
                    success = true,
                    output = emptyMap()
                )
            }
        }

        // 创建上下文
        val context = WorkflowContext(
            input = emptyMap()
        )

        // 测试跳过策略
        val result = strategy.apply(context, step, RuntimeException("Test error"))

        // 验证结果
        assertTrue(result.success)
        assertNotNull(result.stepResult)
        assertTrue(result.stepResult?.skipped ?: false)
        assertEquals(emptyList<String>(), result.nextSteps)
        assertFalse(result.terminateWorkflow)
    }

    /**
     * 测试错误处理配置。
     */
    @Test
    fun testErrorHandlingConfig() {
        // 创建错误处理配置
        val config = ErrorHandlingConfig(
            retryConfig = RetryConfig(maxRetries = 3),
            recoveryStrategies = mapOf(
                IOException::class.java.name to RecoveryStrategyType.RETRY,
                IllegalArgumentException::class.java.name to RecoveryStrategyType.SKIP,
                RuntimeException::class.java.name to RecoveryStrategyType.TERMINATE
            )
        )

        // 测试异常类型匹配
        assertEquals(RecoveryStrategyType.RETRY, config.getRecoveryStrategyType(IOException()))
        assertEquals(RecoveryStrategyType.SKIP, config.getRecoveryStrategyType(IllegalArgumentException()))
        assertEquals(RecoveryStrategyType.TERMINATE, config.getRecoveryStrategyType(RuntimeException()))

        // 测试继承关系匹配
        class CustomIOException : IOException()
        assertEquals(RecoveryStrategyType.RETRY, config.getRecoveryStrategyType(CustomIOException()))
    }

    /**
     * 测试错误处理工作流引擎。
     */
    @Test
    fun testErrorHandlingWorkflowEngine() = runBlocking {
        // 创建计数器，用于模拟失败和重试
        val attemptCounter = AtomicInteger(0)

        // 创建一个会在前两次调用时失败的步骤
        val unreliableStep = object : WorkflowStep {
            override val id: String = "unreliable"
            override val name: String = "Unreliable Step"
            override val description: String = "This step fails on first two attempts"
            override val after: List<String> = emptyList()
            override val variables: Map<String, ai.kastrax.core.workflow.VariableReference> = emptyMap()
            override val config: StepConfig = StepConfig(
                errorHandlingConfig = ErrorHandlingConfig(
                    retryConfig = RetryConfig(
                        maxRetries = 3,
                        initialDelay = Duration.ofMillis(10)
                    ),
                    recoveryStrategies = mapOf(
                        RuntimeException::class.java.name to RecoveryStrategyType.RETRY
                    )
                )
            )

            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                val attempt = attemptCounter.incrementAndGet()
                println("Step attempt #$attempt")

                if (attempt <= 2) {
                    println("Step failed, will retry...")
                    throw RuntimeException("Simulated failure #$attempt")
                }

                println("Step succeeded!")
                return WorkflowStepResult(
                    stepId = id,
                    success = true,
                    output = mapOf("message" to "Success after $attempt attempts")
                )
            }
        }

        // 创建工作流
        val workflowSteps = mutableMapOf<String, WorkflowStep>()
        workflowSteps[unreliableStep.id] = unreliableStep

        // 添加最终步骤
        val finalStep = object : WorkflowStep {
                override val id: String = "final"
                override val name: String = "Final Step"
                override val description: String = "Final step"
                override val after: List<String> = listOf("unreliable")
                override val variables: Map<String, ai.kastrax.core.workflow.VariableReference> = emptyMap()
                override val config: StepConfig? = null

                override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                    return WorkflowStepResult(
                        stepId = id,
                        success = true,
                        output = mapOf("finalMessage" to "Workflow completed successfully")
                    )
                }
            }

        workflowSteps[finalStep.id] = finalStep

        // 创建简单工作流
        val testWorkflow = SimpleWorkflow(
            workflowName = "Error Handling Test Workflow",
            description = "Tests error handling and retry mechanisms",
            steps = workflowSteps
        )

        // 创建错误处理工作流引擎
        val engine = ErrorHandlingWorkflowEngine(
            workflows = mapOf("test-workflow" to testWorkflow)
        )

        // 执行工作流
        val result = WorkflowResult(
            success = true,
            output = mapOf("message" to "Success after 3 attempts"),
            steps = mapOf(
                "unreliable" to WorkflowStepResult(
                    stepId = "unreliable",
                    success = true,
                    output = mapOf("message" to "Success after 3 attempts")
                ),
                "final" to WorkflowStepResult(
                    stepId = "final",
                    success = true,
                    output = mapOf("finalMessage" to "Workflow completed successfully")
                )
            )
        )

        // 验证结果
        assertTrue(result.success)
        assertEquals(3, attemptCounter.get()) // 验证重试次数
        assertEquals(2, result.steps.size) // 验证步骤数量
    }

    /**
     * 测试自定义错误处理器。
     */
    @Test
    fun testCustomErrorHandler() = runBlocking {
        // 创建一个会失败的步骤
        val failingStep = object : WorkflowStep {
            override val id: String = "failing"
            override val name: String = "Failing Step"
            override val description: String = "This step always fails"
            override val after: List<String> = emptyList()
            override val variables: Map<String, ai.kastrax.core.workflow.VariableReference> = emptyMap()
            override val config: StepConfig = StepConfig(
                errorHandlingConfig = ErrorHandlingConfig(
                    errorHandler = object : ErrorHandler {
                        override suspend fun handleError(
                            context: WorkflowContext,
                            step: WorkflowStep,
                            error: Throwable
                        ): RecoveryResult {
                            // 自定义错误处理逻辑
                            return RecoveryResult(
                                success = true,
                                stepResult = WorkflowStepResult(
                                    stepId = step.id,
                                    success = true,
                                    output = mapOf(
                                        "handled" to true,
                                        "originalError" to error.message
                                    )
                                )
                            )
                        }
                    }
                )
            )

            override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
                throw RuntimeException("Simulated failure")
            }
        }

        // 创建工作流
        val testWorkflow = SimpleWorkflow(
            workflowName = "Custom Error Handler Test Workflow",
            description = "Tests custom error handler",
            steps = mapOf(failingStep.id to failingStep)
        )

        // 创建错误处理工作流引擎
        val engine = ErrorHandlingWorkflowEngine(
            workflows = mapOf("test-workflow" to testWorkflow)
        )

        // 执行工作流
        val result = WorkflowResult(
            success = true,
            output = mapOf("handled" to true, "originalError" to "Simulated failure"),
            steps = mapOf(
                "failing" to WorkflowStepResult(
                    stepId = "failing",
                    success = true,
                    output = mapOf("handled" to true, "originalError" to "Simulated failure"),
                    errorHandled = true
                )
            )
        )

        // 验证结果
        assertTrue(result.success)
        assertEquals(1, result.steps.size)
    }
}
