package ai.kastrax.examples.workflow.dataflow

import ai.kastrax.core.workflow.StepConfig
import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowExecuteOptions
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.dataflow.DataTransformStep
import ai.kastrax.core.workflow.dataflow.EnhancedVariableReference
import ai.kastrax.core.workflow.dataflow.EnhancedWorkflowContext
import ai.kastrax.core.workflow.dataflow.SourceType
import ai.kastrax.core.workflow.dataflow.TransformOperationType
import ai.kastrax.core.workflow.dataflow.debug.DataFlowDebugger
import ai.kastrax.core.workflow.dataflow.visualization.DataFlowVisualizer
import ai.kastrax.core.workflow.dataflow.visualization.EnhancedVariableReferenceProvider

import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * 数据流可视化和调试示例。
 */
fun main() = runBlocking {
    println("=== 数据流可视化和调试示例 ===")

    // 创建工作流
    val workflow = createSampleWorkflow()

    // 创建输入数据
    val input = mapOf(
        "users" to listOf(
            mapOf("id" to 1, "name" to "Alice", "age" to 30, "active" to true),
            mapOf("id" to 2, "name" to "Bob", "age" to 25, "active" to false),
            mapOf("id" to 3, "name" to "Charlie", "age" to 35, "active" to true),
            mapOf("id" to 4, "name" to "David", "age" to 40, "active" to true)
        )
    )

    // 创建工作流执行选项
    val options = WorkflowExecuteOptions()

    // 创建增强的工作流上下文
    val enhancedContext = EnhancedWorkflowContext.fromStandardContext(
        context = WorkflowContext(input),
        workflowId = "sample-workflow"
    )

    // 1. 可视化工作流数据流
    println("\n=== 工作流数据流可视化 ===")
    val dataFlowDiagram = enhancedContext.visualizeDataFlow(
        workflow = workflow,
        format = DataFlowVisualizer.VisualizationFormat.MERMAID
    )
    println(dataFlowDiagram)

    // 保存可视化图表到文件
    val visualizer = DataFlowVisualizer()
    visualizer.saveToFile(
        visualization = dataFlowDiagram,
        filePath = "workflow_dataflow",
        format = DataFlowVisualizer.VisualizationFormat.MERMAID
    )
    println("数据流图表已保存到: workflow_dataflow.mmd")

    // 2. 调试工作流执行
    println("\n=== 工作流调试执行 ===")
    val debugResult = enhancedContext.debugWorkflow(
        workflow = workflow,
        mode = DataFlowDebugger.DebugMode.REPORT,
        outputDir = "debug_output"
    )

    println("工作流执行完成，调试报告已生成")

    // 3. 可视化工作流执行数据流
    println("\n=== 工作流执行数据流可视化 ===")
    val executionDataFlowDiagram = enhancedContext.visualizeExecutionDataFlow(
        workflow = workflow,
        format = DataFlowVisualizer.VisualizationFormat.MERMAID,
        includeValues = true
    )
    println(executionDataFlowDiagram)

    // 保存执行可视化图表到文件
    visualizer.saveToFile(
        visualization = executionDataFlowDiagram,
        filePath = "workflow_execution_dataflow",
        format = DataFlowVisualizer.VisualizationFormat.MERMAID
    )
    println("执行数据流图表已保存到: workflow_execution_dataflow.mmd")

    // 4. 检查工作流数据流
    println("\n=== 工作流数据流检查 ===")
    val inspectionResult = enhancedContext.inspectDataFlow(workflow)
    println(inspectionResult.generateReport())

    // 5. 检查工作流执行结果
    println("\n=== 工作流执行结果检查 ===")
    val executionInspectionResult = enhancedContext.inspectExecutionDataFlow(workflow)
    println(executionInspectionResult.generateReport())

    // 6. 跟踪工作流执行数据流
    println("\n=== 工作流执行数据流跟踪 ===")
    val traceResult = enhancedContext.traceDataFlow(workflow)
    println(traceResult.generateReport())

    // 7. 跟踪特定变量
    println("\n=== 变量跟踪 ===")
    val variableTraceResult = enhancedContext.traceVariable(workflow, "activeUsers")
    println(variableTraceResult.generateReport())

    println("\n示例执行完成！")
}

/**
 * 创建示例工作流。
 */
private fun createSampleWorkflow(): Workflow {
    // 步骤1：过滤活跃用户
    val filterActiveUsersStep = DataTransformStep(
        id = "filter_active_users",
        name = "过滤活跃用户",
        description = "过滤出活跃状态的用户",
        operationType = TransformOperationType.FILTER,
        inputReference = EnhancedVariableReference(SourceType.INPUT, "users"),
        outputMapping = mapOf(
            "activeUsers" to EnhancedVariableReference(SourceType.CONSTANT, "result")
        ),
        transformConfig = mapOf(
            "predicate" to EnhancedVariableReference(SourceType.CONSTANT, "lambda: item => item.active === true")
        )
    )

    // 步骤2：计算活跃用户的平均年龄
    val calculateAverageAgeStep = DataTransformStep(
        id = "calculate_average_age",
        name = "计算平均年龄",
        description = "计算活跃用户的平均年龄",
        operationType = TransformOperationType.AGGREGATE,
        inputReference = EnhancedVariableReference(SourceType.STEP, "filter_active_users.activeUsers"),
        outputMapping = mapOf(
            "averageAge" to EnhancedVariableReference(SourceType.CONSTANT, "result")
        ),
        transformConfig = mapOf(
            "initialValue" to EnhancedVariableReference(SourceType.CONSTANT, "0"),
            "operation" to EnhancedVariableReference(
                SourceType.CONSTANT,
                "lambda: (acc, item) => acc + item.age / activeUsers.length"
            )
        ),
        after = listOf("filter_active_users")
    )

    // 步骤3：转换用户数据
    val transformUsersStep = DataTransformStep(
        id = "transform_users",
        name = "转换用户数据",
        description = "转换活跃用户的数据格式",
        operationType = TransformOperationType.MAP,
        inputReference = EnhancedVariableReference(SourceType.STEP, "filter_active_users.activeUsers"),
        outputMapping = mapOf(
            "transformedUsers" to EnhancedVariableReference(SourceType.CONSTANT, "result")
        ),
        transformConfig = mapOf(
            "mapping" to mapOf(
                "userIds" to EnhancedVariableReference(SourceType.STEP, "filter_active_users.activeUsers.id"),
                "userNames" to EnhancedVariableReference(SourceType.STEP, "filter_active_users.activeUsers.name"),
                "averageAge" to EnhancedVariableReference(SourceType.STEP, "calculate_average_age.averageAge")
            )
        ),
        after = listOf("calculate_average_age")
    )

    // 步骤4：生成报告
    val generateReportStep = object : ai.kastrax.core.workflow.WorkflowStep, EnhancedVariableReferenceProvider {
        override val id: String = "generate_report"
        override val name: String = "生成报告"
        override val description: String = "生成用户报告"
        override val after: List<String> = listOf("transform_users")
        override val variables: Map<String, ai.kastrax.core.workflow.VariableReference> = emptyMap()
        override val config: StepConfig? = null

        override suspend fun execute(context: WorkflowContext): WorkflowStepResult {
            // 获取转换后的用户数据
            val transformedUsers = context.steps["transform_users"]?.output?.get("transformedUsers") as? Map<*, *>
            val userIds = transformedUsers?.get("userIds") as? List<*> ?: emptyList<Int>()
            val userNames = transformedUsers?.get("userNames") as? List<*> ?: emptyList<String>()
            val averageAge = transformedUsers?.get("averageAge") as? Number ?: 0

            // 生成报告
            val report = buildString {
                appendLine("=== 用户报告 ===")
                appendLine("活跃用户数: ${userIds.size}")
                appendLine("平均年龄: $averageAge")
                appendLine("用户列表:")

                userIds.zip(userNames).forEachIndexed { index, (id, name) ->
                    appendLine("  ${index + 1}. ID: $id, 姓名: $name")
                }
            }

            // 保存报告到文件
            val reportFile = File("user_report.txt")
            reportFile.writeText(report)

            return WorkflowStepResult(
                stepId = id,
                success = true,
                output = mapOf(
                    "report" to report,
                    "reportFile" to reportFile.absolutePath
                ),
                executionTime = 50
            )
        }

        override fun getEnhancedVariableReferences(): List<EnhancedVariableReference> {
            return listOf(
                EnhancedVariableReference(SourceType.STEP, "transform_users.transformedUsers")
            )
        }
    }

    // 创建工作流
    return object : Workflow {
        override val id: String = "user_analysis_workflow"
        override val name: String = "用户分析工作流"
        override val description: String = "分析用户数据并生成报告"
        override val steps: List<ai.kastrax.core.workflow.WorkflowStep> = listOf(
            filterActiveUsersStep,
            calculateAverageAgeStep,
            transformUsersStep,
            generateReportStep
        )


    }
}
