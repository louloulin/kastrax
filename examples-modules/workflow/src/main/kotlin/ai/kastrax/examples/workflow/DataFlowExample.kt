package ai.kastrax.examples.workflow

import ai.kastrax.core.workflow.StepConfig
import ai.kastrax.core.workflow.VariableReference
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStep
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.SimpleWorkflow
import ai.kastrax.core.workflow.dataflow.DataTransformStep
import ai.kastrax.core.workflow.dataflow.DataTransformer
import ai.kastrax.core.workflow.dataflow.EnhancedVariableReference
import ai.kastrax.core.workflow.dataflow.EnhancedWorkflowContext
import ai.kastrax.core.workflow.dataflow.SourceType
import ai.kastrax.core.workflow.dataflow.TransformOperationType
import ai.kastrax.core.workflow.dataflow.TransformType
import ai.kastrax.core.workflow.dataflow.VariableScopeManager
import ai.kastrax.core.workflow.engine.WorkflowEngine
import kotlinx.coroutines.runBlocking

/**
 * 数据流和变量处理示例。
 */
fun main() = runBlocking {
    println("开始数据流和变量处理示例...")

    // 创建数据生成步骤
    val dataGenerationStep = object : WorkflowStep {
        override val id: String = "generate_data"
        override val name: String = "Generate Data"
        override val description: String = "Generate sample data"
        override val after: List<String> = emptyList()
        override val variables: Map<String, VariableReference> = emptyMap()
        override val config: StepConfig? = null

        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("生成示例数据...")

            // 生成用户数据列表
            val users = listOf(
                mapOf(
                    "id" to 1,
                    "name" to "John Doe",
                    "age" to 30,
                    "email" to "john@example.com",
                    "active" to true,
                    "tags" to listOf("customer", "premium")
                ),
                mapOf(
                    "id" to 2,
                    "name" to "Jane Smith",
                    "age" to 25,
                    "email" to "jane@example.com",
                    "active" to false,
                    "tags" to listOf("customer")
                ),
                mapOf(
                    "id" to 3,
                    "name" to "Bob Johnson",
                    "age" to 40,
                    "email" to "bob@example.com",
                    "active" to true,
                    "tags" to listOf("customer", "premium", "vip")
                ),
                mapOf(
                    "id" to 4,
                    "name" to "Alice Brown",
                    "age" to 35,
                    "email" to "alice@example.com",
                    "active" to true,
                    "tags" to listOf("customer")
                ),
                mapOf(
                    "id" to 5,
                    "name" to "Charlie Wilson",
                    "age" to 28,
                    "email" to "charlie@example.com",
                    "active" to false,
                    "tags" to listOf("customer", "premium")
                )
            )

            // 创建增强的工作流上下文
            val enhancedContext = EnhancedWorkflowContext.fromStandardContext(
                context = context,
                workflowId = "data_flow_example"
            )

            // 设置全局变量
            enhancedContext.setGlobalVariable("userCount", users.size)
            enhancedContext.setGlobalVariable("appName", "DataFlow Example")

            // 设置工作流变量
            enhancedContext.setWorkflowVariable("processingDate", java.time.LocalDate.now().toString())

            // 设置步骤变量
            enhancedContext.setStepVariable(id, "generatedAt", System.currentTimeMillis())

            return WorkflowStepResult(
                stepId = id,
                success = true,
                output = mapOf(
                    "users" to users,
                    "metadata" to mapOf(
                        "count" to users.size,
                        "source" to "sample",
                        "timestamp" to System.currentTimeMillis()
                    )
                )
            )
        }
    }

    // 创建数据过滤步骤
    val filterStep = DataTransformStep(
        id = "filter_active_users",
        name = "Filter Active Users",
        description = "Filter only active users",
        operationType = TransformOperationType.FILTER,
        inputReference = EnhancedVariableReference.step("generate_data", "users"),
        transformConfig = mapOf(
            "predicate" to EnhancedVariableReference(
                source = SourceType.CONSTANT,
                path = "lambda: user -> (user as Map<*, *>)[\"active\"] == true"
            )
        ),
        after = listOf("generate_data")
    )

    // 创建数据映射步骤
    val mapStep = DataTransformStep(
        id = "map_user_profiles",
        name = "Map User Profiles",
        description = "Map users to profile format",
        operationType = TransformOperationType.MAP,
        inputReference = EnhancedVariableReference.step("filter_active_users", "result"),
        transformConfig = mapOf(
            "mapping" to mapOf(
                "profiles" to EnhancedVariableReference(
                    source = SourceType.STEP,
                    path = "filter_active_users.result"
                )
            )
        ),
        outputMapping = mapOf(
            "userProfiles" to EnhancedVariableReference(
                source = SourceType.CONSTANT,
                path = "profiles"
            )
        ),
        after = listOf("filter_active_users")
    )

    // 创建数据聚合步骤
    val aggregateStep = DataTransformStep(
        id = "calculate_average_age",
        name = "Calculate Average Age",
        description = "Calculate the average age of active users",
        operationType = TransformOperationType.AGGREGATE,
        inputReference = EnhancedVariableReference.step("filter_active_users", "result"),
        transformConfig = mapOf(
            "initialValue" to EnhancedVariableReference(
                source = SourceType.CONSTANT,
                path = "0"
            ),
            "operation" to EnhancedVariableReference(
                source = SourceType.CONSTANT,
                path = "lambda: (acc, user) -> { val sum = acc as Int; val age = (user as Map<*, *>)[\"age\"] as Int; sum + age }"
            )
        ),
        after = listOf("filter_active_users")
    )

    // 创建结果汇总步骤
    val summaryStep = object : WorkflowStep {
        override val id: String = "generate_summary"
        override val name: String = "Generate Summary"
        override val description: String = "Generate summary of processed data"
        override val after: List<String> = listOf("map_user_profiles", "calculate_average_age")
        override val variables: Map<String, VariableReference> = emptyMap()
        override val config: StepConfig? = null

        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            println("生成数据处理摘要...")

            // 获取前面步骤的结果
            val allUsers = context.getStepOutput("generate_data")?.get("users") as? List<*> ?: emptyList<Map<*, *>>()
            val activeUsers = context.getStepOutput("filter_active_users")?.get("result") as? List<*> ?: emptyList<Map<*, *>>()
            val userProfiles = context.getStepOutput("map_user_profiles")?.get("userProfiles") as? List<*> ?: emptyList<Map<*, *>>()
            val totalAgeSum = context.getStepOutput("calculate_average_age")?.get("result") as? Int ?: 0

            // 计算平均年龄
            val averageAge = if (activeUsers.isNotEmpty()) {
                totalAgeSum.toDouble() / activeUsers.size
            } else {
                0.0
            }

            // 创建增强的工作流上下文
            val enhancedContext = EnhancedWorkflowContext.fromStandardContext(
                context = context,
                workflowId = "data_flow_example"
            )

            // 获取变量
            val userCount = enhancedContext.getGlobalVariable("userCount") as? Int ?: 0
            val appName = enhancedContext.getGlobalVariable("appName") as? String ?: ""
            val processingDate = enhancedContext.getWorkflowVariable("processingDate") as? String ?: ""

            // 创建摘要
            val summary = """
                |数据处理摘要:
                |应用名称: $appName
                |处理日期: $processingDate
                |总用户数: ${allUsers.size}
                |活跃用户数: ${activeUsers.size}
                |活跃用户百分比: ${(activeUsers.size * 100.0 / allUsers.size).toInt()}%
                |活跃用户平均年龄: ${"%.1f".format(averageAge)}
                |高级用户数: ${activeUsers.count { user ->
                    ((user as Map<*, *>)["tags"] as? List<*>)?.contains("premium") == true
                }}
            """.trimMargin()

            println(summary)

            return WorkflowStepResult(
                stepId = id,
                success = true,
                output = mapOf(
                    "summary" to summary,
                    "statistics" to mapOf(
                        "totalUsers" to allUsers.size,
                        "activeUsers" to activeUsers.size,
                        "averageAge" to averageAge,
                        "premiumUsers" to activeUsers.count { user ->
                            ((user as Map<*, *>)["tags"] as? List<*>)?.contains("premium") == true
                        }
                    )
                )
            )
        }
    }

    // 创建工作流
    val steps = mapOf(
        dataGenerationStep.id to dataGenerationStep,
        filterStep.id to filterStep,
        mapStep.id to mapStep,
        aggregateStep.id to aggregateStep,
        summaryStep.id to summaryStep
    )

    val dataFlowWorkflow = SimpleWorkflow(
        workflowName = "数据流示例工作流",
        description = "演示数据流和变量处理功能",
        steps = steps
    )

    // 创建工作流引擎
    val workflowEngine = WorkflowEngine(
        workflows = mapOf("data_flow_example" to dataFlowWorkflow)
    )

    // 执行工作流
    println("\n开始执行工作流...")
    val result = workflowEngine.executeWorkflow(
        workflowId = "data_flow_example",
        input = emptyMap()
    )

    // 检查工作流执行结果
    println("\n工作流执行结果:")
    println("成功: ${result.success}")
    println("执行时间: ${result.executionTime}ms")
    println("步骤数: ${result.steps.size}")

    // 打印最终统计信息
    val statistics = result.steps["generate_summary"]?.output?.get("statistics") as? Map<*, *>
    if (statistics != null) {
        println("\n统计信息:")
        println("总用户数: ${statistics["totalUsers"]}")
        println("活跃用户数: ${statistics["activeUsers"]}")
        println("平均年龄: ${statistics["averageAge"]}")
        println("高级用户数: ${statistics["premiumUsers"]}")
    }
}
