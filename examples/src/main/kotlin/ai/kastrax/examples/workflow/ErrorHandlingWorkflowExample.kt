package ai.kastrax.examples.workflow

import ai.kastrax.core.agent.agent
import ai.kastrax.core.workflow.RetryConfig
import ai.kastrax.core.workflow.StepConfig
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.SimpleWorkflow
import ai.kastrax.core.workflow.error.ErrorHandlingConfig
import ai.kastrax.core.workflow.error.ErrorHandlingWorkflowEngine
import ai.kastrax.core.workflow.error.RecoveryStrategyType
import ai.kastrax.integrations.openai.openAi
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.ConnectException
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/**
 * 错误处理工作流示例。
 */
fun main() = runBlocking {
    println("开始错误处理工作流示例...")

    // 创建计数器，用于模拟失败和重试
    val networkErrorCounter = AtomicInteger(0)
    val validationErrorCounter = AtomicInteger(0)
    val processingErrorCounter = AtomicInteger(0)

    // 创建一个会产生网络错误的步骤
    val networkErrorStep = object : WorkflowStep {
        override val id: String = "network_error"
        override val name: String = "Network Error Step"
        override val description: String = "This step simulates network errors"
        override val after: List<String> = emptyList()
        override val variables: Map<String, VariableReference> = emptyMap()
        override val config: StepConfig = StepConfig(
            errorHandlingConfig = ErrorHandlingConfig(
                retryConfig = RetryConfig(
                    maxRetries = 3,
                    initialDelay = Duration.ofMillis(100),
                    maxDelay = Duration.ofSeconds(1),
                    backoffFactor = 2.0,
                    jitter = 0.1
                ),
                recoveryStrategies = mapOf(
                    IOException::class.java.name to RecoveryStrategyType.RETRY,
                    ConnectException::class.java.name to RecoveryStrategyType.RETRY
                )
            )
        )

        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            val attempt = networkErrorCounter.incrementAndGet()
            println("网络错误步骤尝试 #$attempt")

            if (attempt <= 2) {
                println("网络错误，将重试...")
                throw ConnectException("模拟的网络错误 #$attempt")
            }

            println("网络步骤成功！")
            return WorkflowStepResult(
                stepId = id,
                success = true,
                output = mapOf("message" to "网络连接成功，尝试次数: $attempt")
            )
        }
    }

    // 创建一个会产生验证错误的步骤
    val validationErrorStep = object : WorkflowStep {
        override val id: String = "validation_error"
        override val name: String = "Validation Error Step"
        override val description: String = "This step simulates validation errors"
        override val after: List<String> = listOf("network_error")
        override val variables: Map<String, VariableReference> = emptyMap()
        override val config: StepConfig = StepConfig(
            errorHandlingConfig = ErrorHandlingConfig(
                recoveryStrategies = mapOf(
                    IllegalArgumentException::class.java.name to RecoveryStrategyType.SKIP
                )
            )
        )

        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            val attempt = validationErrorCounter.incrementAndGet()
            println("验证错误步骤尝试 #$attempt")

            // 始终抛出验证错误
            throw IllegalArgumentException("模拟的验证错误")
        }
    }

    // 创建一个会产生处理错误的步骤
    val processingErrorStep = object : WorkflowStep {
        override val id: String = "processing_error"
        override val name: String = "Processing Error Step"
        override val description: String = "This step simulates processing errors"
        override val after: List<String> = listOf("validation_error")
        override val variables: Map<String, VariableReference> = emptyMap()
        override val config: StepConfig = StepConfig(
            errorHandlingConfig = ErrorHandlingConfig(
                recoveryStrategies = mapOf(
                    RuntimeException::class.java.name to RecoveryStrategyType.FALLBACK
                )
            )
        )

        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            val attempt = processingErrorCounter.incrementAndGet()
            println("处理错误步骤尝试 #$attempt")

            // 始终抛出处理错误
            throw RuntimeException("模拟的处理错误")
        }
    }

    // 创建最终步骤
    val finalStep = object : WorkflowStep {
        override val id: String = "final"
        override val name: String = "Final Step"
        override val description: String = "Final step"
        override val after: List<String> = listOf("processing_error")
        override val variables: Map<String, VariableReference> = emptyMap()
        override val config: StepConfig? = null

        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("执行最终步骤...")

            // 获取前面步骤的结果
            val networkResult = context.getStepOutput("network_error")
            val validationResult = context.getStepOutput("validation_error")
            val processingResult = context.getStepOutput("processing_error")

            println("网络步骤结果: $networkResult")
            println("验证步骤结果: $validationResult")
            println("处理步骤结果: $processingResult")

            return WorkflowStepResult(
                stepId = id,
                success = true,
                output = mapOf(
                    "summary" to "工作流完成，处理了所有错误",
                    "networkAttempts" to networkErrorCounter.get(),
                    "validationAttempts" to validationErrorCounter.get(),
                    "processingAttempts" to processingErrorCounter.get()
                )
            )
        }
    }

    // 创建工作流
    val steps = mapOf(
        networkErrorStep.id to networkErrorStep,
        validationErrorStep.id to validationErrorStep,
        processingErrorStep.id to processingErrorStep,
        finalStep.id to finalStep
    )

    val errorHandlingWorkflow = SimpleWorkflow(
        workflowName = "错误处理示例工作流",
        description = "演示不同类型的错误处理策略",
        steps = steps
    )

    // 创建全局错误处理配置
    val globalErrorHandlingConfig = ErrorHandlingConfig(
        retryConfig = RetryConfig(
            maxRetries = 2,
            initialDelay = Duration.ofMillis(100)
        ),
        recoveryStrategies = mapOf(
            Exception::class.java.name to RecoveryStrategyType.TERMINATE
        )
    )

    // 创建错误处理工作流引擎
    val workflowEngine = ErrorHandlingWorkflowEngine(
        workflows = mapOf("ErrorHandlingWorkflow" to errorHandlingWorkflow),
        globalErrorHandlingConfig = globalErrorHandlingConfig
    )

    // 执行工作流
    println("\n开始执行工作流...")
    val result = workflowEngine.executeWorkflow(
        workflowId = "ErrorHandlingWorkflow",
        input = emptyMap()
    )

    // 检查工作流执行结果
    println("\n工作流执行结果:")
    println("成功: ${result.success}")
    println("输出: ${result.output}")
    println("执行时间: ${result.executionTime}ms")
    println("步骤数: ${result.steps.size}")

    // 打印每个步骤的结果
    println("\n步骤结果:")
    result.steps.forEach { (stepId, stepResult) ->
        println("步骤: $stepId")
        println("  成功: ${stepResult.success}")
        println("  输出: ${stepResult.output}")
        println("  错误: ${stepResult.error}")
    }

    // 打印总结
    println("\n总结:")
    println("网络错误步骤尝试次数: ${networkErrorCounter.get()}")
    println("验证错误步骤尝试次数: ${validationErrorCounter.get()}")
    println("处理错误步骤尝试次数: ${processingErrorCounter.get()}")
}
