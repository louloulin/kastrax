package ai.kastrax.core.workflow.dataflow.visualization

import ai.kastrax.core.workflow.Workflow
import ai.kastrax.core.workflow.WorkflowContext
import ai.kastrax.core.workflow.WorkflowStepResult
import ai.kastrax.core.workflow.dataflow.EnhancedVariableReference
import ai.kastrax.core.workflow.dataflow.EnhancedWorkflowContext
import ai.kastrax.core.workflow.dataflow.SourceType
import mu.KotlinLogging
import java.io.File

/**
 * 数据流可视化器，用于生成工作流数据流的可视化表示。
 */
class DataFlowVisualizer {
    private val logger = KotlinLogging.logger {}

    /**
     * 可视化格式枚举。
     */
    enum class VisualizationFormat {
        /**
         * DOT格式，用于Graphviz。
         */
        DOT,

        /**
         * Mermaid格式，用于Markdown。
         */
        MERMAID,

        /**
         * JSON格式。
         */
        JSON,

        /**
         * 纯文本格式。
         */
        TEXT
    }

    /**
     * 生成工作流数据流的可视化表示。
     *
     * @param workflow 工作流
     * @param format 可视化格式
     * @param includeValues 是否包含变量值
     * @return 可视化表示
     */
    fun visualize(
        workflow: Workflow,
        format: VisualizationFormat = VisualizationFormat.MERMAID,
        includeValues: Boolean = false
    ): String {
        return when (format) {
            VisualizationFormat.DOT -> generateDotGraph(workflow, includeValues)
            VisualizationFormat.MERMAID -> generateMermaidDiagram(workflow, includeValues)
            VisualizationFormat.JSON -> generateJsonRepresentation(workflow, includeValues)
            VisualizationFormat.TEXT -> generateTextRepresentation(workflow, includeValues)
        }
    }

    /**
     * 生成工作流执行的数据流可视化表示。
     *
     * @param workflow 工作流
     * @param context 工作流上下文
     * @param format 可视化格式
     * @param includeValues 是否包含变量值
     * @return 可视化表示
     */
    fun visualizeExecution(
        workflow: Workflow,
        context: WorkflowContext,
        format: VisualizationFormat = VisualizationFormat.MERMAID,
        includeValues: Boolean = true
    ): String {
        return when (format) {
            VisualizationFormat.DOT -> generateExecutionDotGraph(workflow, context, includeValues)
            VisualizationFormat.MERMAID -> generateExecutionMermaidDiagram(workflow, context, includeValues)
            VisualizationFormat.JSON -> generateExecutionJsonRepresentation(workflow, context, includeValues)
            VisualizationFormat.TEXT -> generateExecutionTextRepresentation(workflow, context, includeValues)
        }
    }

    /**
     * 将可视化表示保存到文件。
     *
     * @param visualization 可视化表示
     * @param filePath 文件路径
     * @param format 可视化格式
     */
    fun saveToFile(
        visualization: String,
        filePath: String,
        format: VisualizationFormat = VisualizationFormat.MERMAID
    ) {
        val extension = when (format) {
            VisualizationFormat.DOT -> "dot"
            VisualizationFormat.MERMAID -> "mmd"
            VisualizationFormat.JSON -> "json"
            VisualizationFormat.TEXT -> "txt"
        }
        val file = File("$filePath.$extension")
        file.writeText(visualization)
        logger.info { "数据流可视化已保存到: ${file.absolutePath}" }
    }

    /**
     * 生成DOT格式的图形表示。
     */
    private fun generateDotGraph(workflow: Workflow, includeValues: Boolean): String {
        val sb = StringBuilder()
        sb.appendLine("digraph DataFlow {")
        sb.appendLine("  rankdir=LR;")
        sb.appendLine("  node [shape=box, style=filled, fillcolor=lightblue];")
        sb.appendLine("  edge [color=gray];")

        // 添加工作流输入节点
        sb.appendLine("  input [label=\"Input\", shape=ellipse, fillcolor=lightgreen];")

        // 添加步骤节点
        val steps = getWorkflowSteps(workflow)
        steps.forEach { step ->
            sb.appendLine("  step_${step.id} [label=\"${step.name}\"];")
        }

        // 添加工作流输出节点
        sb.appendLine("  output [label=\"Output\", shape=ellipse, fillcolor=lightyellow];")

        // 添加数据流边
        steps.forEach { step ->
            // 分析步骤的变量引用，找出数据依赖
            analyzeStepDependencies(step).forEach { dependency ->
                val sourceId = dependency.first
                val targetId = dependency.second
                if (sourceId == "input") {
                    sb.appendLine("  input -> step_$targetId;")
                } else {
                    sb.appendLine("  step_$sourceId -> step_$targetId;")
                }
            }

            // 如果是最后一个步骤，连接到输出
            if (steps.none { s -> s.after.contains(step.id) }) {
                sb.appendLine("  step_${step.id} -> output;")
            }
        }

        sb.appendLine("}")
        return sb.toString()
    }

    /**
     * 生成Mermaid格式的图表。
     */
    private fun generateMermaidDiagram(workflow: Workflow, includeValues: Boolean): String {
        val sb = StringBuilder()
        sb.appendLine("```mermaid")
        sb.appendLine("graph LR")

        // 添加工作流输入节点
        sb.appendLine("  input((\"Input\"))")

        // 添加步骤节点
        val steps = getWorkflowSteps(workflow)
        steps.forEach { step ->
            sb.appendLine("  step_${step.id}[\"${step.name}\"]")
        }

        // 添加工作流输出节点
        sb.appendLine("  output((\"Output\"))")

        // 添加数据流边
        steps.forEach { step ->
            // 分析步骤的变量引用，找出数据依赖
            analyzeStepDependencies(step).forEach { dependency ->
                val sourceId = dependency.first
                val targetId = dependency.second
                if (sourceId == "input") {
                    sb.appendLine("  input --> step_$targetId")
                } else {
                    sb.appendLine("  step_$sourceId --> step_$targetId")
                }
            }

            // 如果是最后一个步骤，连接到输出
            if (steps.none { s -> s.after.contains(step.id) }) {
                sb.appendLine("  step_${step.id} --> output")
            }
        }

        sb.appendLine("```")
        return sb.toString()
    }

    /**
     * 生成JSON格式的表示。
     */
    private fun generateJsonRepresentation(workflow: Workflow, includeValues: Boolean): String {
        val nodes = mutableListOf<Map<String, Any>>()
        val edges = mutableListOf<Map<String, Any>>()

        // 添加工作流输入节点
        nodes.add(mapOf(
            "id" to "input",
            "type" to "input",
            "label" to "Input"
        ))

        // 添加步骤节点
        val steps = getWorkflowSteps(workflow)
        steps.forEach { step ->
            nodes.add(mapOf(
                "id" to "step_${step.id}",
                "type" to "step",
                "label" to step.name,
                "stepId" to step.id
            ))
        }

        // 添加工作流输出节点
        nodes.add(mapOf(
            "id" to "output",
            "type" to "output",
            "label" to "Output"
        ))

        // 添加数据流边
        steps.forEach { step ->
            // 分析步骤的变量引用，找出数据依赖
            analyzeStepDependencies(step).forEach { dependency ->
                val sourceId = dependency.first
                val targetId = dependency.second
                if (sourceId == "input") {
                    edges.add(mapOf(
                        "source" to "input",
                        "target" to "step_$targetId"
                    ))
                } else {
                    edges.add(mapOf(
                        "source" to "step_$sourceId",
                        "target" to "step_$targetId"
                    ))
                }
            }

            // 如果是最后一个步骤，连接到输出
            if (steps.none { s -> s.after.contains(step.id) }) {
                edges.add(mapOf(
                    "source" to "step_${step.id}",
                    "target" to "output"
                ))
            }
        }

        val result = mapOf(
            "nodes" to nodes,
            "edges" to edges
        )

        return result.toString()
    }

    /**
     * 生成纯文本格式的表示。
     */
    private fun generateTextRepresentation(workflow: Workflow, includeValues: Boolean): String {
        val sb = StringBuilder()
        sb.appendLine("=== 工作流数据流 ===")
        sb.appendLine("工作流: ${workflow.javaClass.simpleName}")
        sb.appendLine()

        val steps = getWorkflowSteps(workflow)

        sb.appendLine("输入 -> 步骤:")
        steps.forEach { step ->
            val inputDependencies = analyzeStepDependencies(step).filter { it.first == "input" }
            if (inputDependencies.isNotEmpty()) {
                sb.appendLine("  输入 -> ${step.name} (${step.id})")
            }
        }
        sb.appendLine()

        sb.appendLine("步骤间数据流:")
        steps.forEach { step ->
            val stepDependencies = analyzeStepDependencies(step).filter { it.first != "input" }
            stepDependencies.forEach { dependency ->
                val sourceId = dependency.first
                val sourceStep = steps.find { it.id == sourceId }
                if (sourceStep != null) {
                    sb.appendLine("  ${sourceStep.name} (${sourceStep.id}) -> ${step.name} (${step.id})")
                }
            }
        }
        sb.appendLine()

        sb.appendLine("步骤 -> 输出:")
        steps.forEach { step ->
            if (steps.none { s -> s.after.contains(step.id) }) {
                sb.appendLine("  ${step.name} (${step.id}) -> 输出")
            }
        }

        return sb.toString()
    }

    /**
     * 生成执行时的DOT格式图形表示。
     */
    private fun generateExecutionDotGraph(
        workflow: Workflow,
        context: WorkflowContext,
        includeValues: Boolean
    ): String {
        val sb = StringBuilder()
        sb.appendLine("digraph DataFlowExecution {")
        sb.appendLine("  rankdir=LR;")
        sb.appendLine("  node [shape=box, style=filled];")
        sb.appendLine("  edge [color=gray];")

        // 添加工作流输入节点
        sb.appendLine("  input [label=\"Input\", shape=ellipse, fillcolor=lightgreen];")

        // 添加步骤节点
        val steps = getWorkflowSteps(workflow)
        steps.forEach { step ->
            val result = context.steps[step.id]
            val color = when {
                result == null -> "white"
                result.success -> "lightblue"
                else -> "lightcoral"
            }

            val label = if (includeValues && result != null) {
                "${step.name}\\n${formatStepResult(result)}"
            } else {
                step.name
            }

            sb.appendLine("  step_${step.id} [label=\"$label\", fillcolor=$color];")
        }

        // 添加工作流输出节点
        sb.appendLine("  output [label=\"Output\", shape=ellipse, fillcolor=lightyellow];")

        // 添加数据流边
        steps.forEach { step ->
            // 分析步骤的变量引用，找出数据依赖
            analyzeStepDependencies(step).forEach { dependency ->
                val sourceId = dependency.first
                val targetId = dependency.second
                if (sourceId == "input") {
                    sb.appendLine("  input -> step_$targetId;")
                } else {
                    sb.appendLine("  step_$sourceId -> step_$targetId;")
                }
            }

            // 如果是最后一个步骤，连接到输出
            if (steps.none { s -> s.after.contains(step.id) }) {
                sb.appendLine("  step_${step.id} -> output;")
            }
        }

        sb.appendLine("}")
        return sb.toString()
    }

    /**
     * 生成执行时的Mermaid格式图表。
     */
    private fun generateExecutionMermaidDiagram(
        workflow: Workflow,
        context: WorkflowContext,
        includeValues: Boolean
    ): String {
        val sb = StringBuilder()
        sb.appendLine("```mermaid")
        sb.appendLine("graph LR")

        // 添加工作流输入节点
        sb.appendLine("  input((\"Input\"))")

        // 添加步骤节点
        val steps = getWorkflowSteps(workflow)
        steps.forEach { step ->
            val result = context.steps[step.id]
            val style = when {
                result == null -> "style=dashed"
                result.success -> "style=filled,fill=#d0e0ff"
                else -> "style=filled,fill=#ffcccc"
            }

            val label = if (includeValues && result != null) {
                "${step.name}<br/>${formatStepResultForMermaid(result)}"
            } else {
                step.name
            }

            sb.appendLine("  step_${step.id}[\"$label\"]")
            sb.appendLine("  style step_${step.id} $style")
        }

        // 添加工作流输出节点
        sb.appendLine("  output((\"Output\"))")

        // 添加数据流边
        steps.forEach { step ->
            // 分析步骤的变量引用，找出数据依赖
            analyzeStepDependencies(step).forEach { dependency ->
                val sourceId = dependency.first
                val targetId = dependency.second
                if (sourceId == "input") {
                    sb.appendLine("  input --> step_$targetId")
                } else {
                    sb.appendLine("  step_$sourceId --> step_$targetId")
                }
            }

            // 如果是最后一个步骤，连接到输出
            if (steps.none { s -> s.after.contains(step.id) }) {
                sb.appendLine("  step_${step.id} --> output")
            }
        }

        sb.appendLine("```")
        return sb.toString()
    }

    /**
     * 生成执行时的JSON格式表示。
     */
    private fun generateExecutionJsonRepresentation(
        workflow: Workflow,
        context: WorkflowContext,
        includeValues: Boolean
    ): String {
        val nodes = mutableListOf<Map<String, Any>>()
        val edges = mutableListOf<Map<String, Any>>()

        // 添加工作流输入节点
        nodes.add(mapOf(
            "id" to "input",
            "type" to "input",
            "label" to "Input",
            "data" to (if (includeValues) context.input else emptyMap<String, Any?>())
        ))

        // 添加步骤节点
        val steps = getWorkflowSteps(workflow)
        steps.forEach { step ->
            val result = context.steps[step.id]
            val status = when {
                result == null -> "pending"
                result.success -> "success"
                else -> "error"
            }

            val nodeData = mutableMapOf<String, Any?>(
                "id" to "step_${step.id}",
                "type" to "step",
                "label" to step.name,
                "stepId" to step.id,
                "status" to status
            )

            if (includeValues && result != null) {
                nodeData["output"] = result.output
                nodeData["error"] = result.error
                nodeData["executionTime"] = result.executionTime
            }

            nodes.add(nodeData as Map<String, Any>)
        }

        // 添加工作流输出节点
        nodes.add(mapOf(
            "id" to "output",
            "type" to "output",
            "label" to "Output"
        ))

        // 添加数据流边
        steps.forEach { step ->
            // 分析步骤的变量引用，找出数据依赖
            analyzeStepDependencies(step).forEach { dependency ->
                val sourceId = dependency.first
                val targetId = dependency.second
                if (sourceId == "input") {
                    edges.add(mapOf(
                        "source" to "input",
                        "target" to "step_$targetId"
                    ))
                } else {
                    edges.add(mapOf(
                        "source" to "step_$sourceId",
                        "target" to "step_$targetId"
                    ))
                }
            }

            // 如果是最后一个步骤，连接到输出
            if (steps.none { s -> s.after.contains(step.id) }) {
                edges.add(mapOf(
                    "source" to "step_${step.id}",
                    "target" to "output"
                ))
            }
        }

        val result = mapOf(
            "nodes" to nodes,
            "edges" to edges
        )

        return result.toString()
    }

    /**
     * 生成执行时的纯文本格式表示。
     */
    private fun generateExecutionTextRepresentation(
        workflow: Workflow,
        context: WorkflowContext,
        includeValues: Boolean
    ): String {
        val sb = StringBuilder()
        sb.appendLine("=== 工作流执行数据流 ===")
        sb.appendLine("工作流: ${workflow.javaClass.simpleName}")
        sb.appendLine()

        if (includeValues) {
            sb.appendLine("输入:")
            context.input.forEach { (key, value) ->
                sb.appendLine("  $key = $value")
            }
            sb.appendLine()
        }

        sb.appendLine("步骤执行:")
        val steps = getWorkflowSteps(workflow)
        steps.forEach { step ->
            val result = context.steps[step.id]
            val status = when {
                result == null -> "待执行"
                result.success -> "成功"
                else -> "失败"
            }

            sb.appendLine("  ${step.name} (${step.id}): $status")

            if (includeValues && result != null) {
                sb.appendLine("    执行时间: ${result.executionTime}ms")
                if (result.error != null) {
                    sb.appendLine("    错误: ${result.error}")
                }
                sb.appendLine("    输出:")
                result.output.forEach { (key, value) ->
                    sb.appendLine("      $key = $value")
                }
            }
        }
        sb.appendLine()

        sb.appendLine("数据流:")
        steps.forEach { step ->
            // 分析步骤的变量引用，找出数据依赖
            analyzeStepDependencies(step).forEach { dependency ->
                val sourceId = dependency.first
                val targetId = dependency.second
                if (sourceId == "input") {
                    sb.appendLine("  输入 -> ${step.name} (${step.id})")
                } else {
                    val sourceStep = steps.find { it.id == sourceId }
                    if (sourceStep != null) {
                        sb.appendLine("  ${sourceStep.name} (${sourceStep.id}) -> ${step.name} (${step.id})")
                    }
                }
            }
        }

        return sb.toString()
    }

    /**
     * 分析步骤依赖关系。
     *
     * @param step 工作流步骤
     * @return 依赖关系列表，每个元素是一个Pair，first是源步骤ID，second是目标步骤ID
     */
    private fun analyzeStepDependencies(step: ai.kastrax.core.workflow.WorkflowStep): List<Pair<String, String>> {
        val dependencies = mutableListOf<Pair<String, String>>()

        // 添加显式依赖（after关系）
        step.after.forEach { afterStepId: String ->
            dependencies.add(Pair(afterStepId, step.id))
        }

        // 分析变量引用，找出隐式依赖
        step.variables.values.forEach { reference: ai.kastrax.core.workflow.VariableReference ->
            if (reference.path.startsWith("$.steps.")) {
                val parts = reference.path.removePrefix("$.steps.").split(".", limit = 2)
                if (parts.isNotEmpty()) {
                    val sourceStepId = parts[0]
                    dependencies.add(Pair(sourceStepId, step.id))
                }
            } else if (reference.path.startsWith("$.input.")) {
                dependencies.add(Pair("input", step.id))
            }
        }

        // 如果步骤实现了EnhancedVariableReference，分析更多依赖
        if (step is EnhancedVariableReferenceProvider) {
            step.getEnhancedVariableReferences().forEach { reference: EnhancedVariableReference ->
                val source = reference.source
                if (source == SourceType.STEP) {
                    val parts = reference.path.split(".", limit = 2)
                    if (parts.isNotEmpty()) {
                        val sourceStepId = parts[0]
                        dependencies.add(Pair(sourceStepId, step.id))
                    }
                } else if (source == SourceType.INPUT) {
                    dependencies.add(Pair("input", step.id))
                } else {
                    /* 忽略其他类型 */
                }
            }
        }

        return dependencies.distinct()
    }

    /**
     * 格式化步骤执行结果，用于DOT图。
     */
    private fun formatStepResult(result: WorkflowStepResult): String {
        val sb = StringBuilder()
        if (result.success) {
            sb.append("Success")
        } else {
            sb.append("Failed: ${result.error}")
        }
        sb.append("\\n")
        sb.append("Time: ${result.executionTime}ms")
        return sb.toString()
    }

    /**
     * 格式化步骤执行结果，用于Mermaid图。
     */
    private fun formatStepResultForMermaid(result: WorkflowStepResult): String {
        val sb = StringBuilder()
        if (result.success) {
            sb.append("Success")
        } else {
            sb.append("Failed: ${result.error}")
        }
        sb.append("<br/>")
        sb.append("Time: ${result.executionTime}ms")
        return sb.toString()
    }
}

/**
 * 增强变量引用提供者接口，用于获取步骤中的增强变量引用。
 */
interface EnhancedVariableReferenceProvider {
    /**
     * 获取步骤中的增强变量引用。
     *
     * @return 增强变量引用列表
     */
    fun getEnhancedVariableReferences(): List<EnhancedVariableReference>
}

/**
 * 获取工作流中的步骤列表。
 * 由于Workflow接口在不同版本中可能有不同的实现方式，这个方法用于兼容不同的实现。
 */
fun getWorkflowSteps(workflow: Workflow): List<ai.kastrax.core.workflow.WorkflowStep> {
    // 尝试获取steps属性
    return try {
        // 先尝试直接访问属性
        val stepsField = workflow.javaClass.getDeclaredField("steps")
        stepsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        stepsField.get(workflow) as? List<ai.kastrax.core.workflow.WorkflowStep> ?: emptyList()
    } catch (e: Exception) {
        // 如果无法获取，则返回空列表
        emptyList()
    }
}
