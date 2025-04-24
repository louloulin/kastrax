package ai.kastrax.core.workflow.visualization

import ai.kastrax.core.workflow.monitoring.ExecutionMetrics
import ai.kastrax.core.workflow.monitoring.StepMetrics
import ai.kastrax.core.workflow.monitoring.StepStatus
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * Generates graph data for workflow visualization.
 */
class WorkflowGraphGenerator {
    /**
     * Generates a graph representation of a workflow.
     *
     * @param workflowId The ID of the workflow.
     * @param steps The steps in the workflow.
     * @param connections The connections between steps.
     * @param format The format of the visualization.
     * @param executionMetrics Optional execution metrics to include in the visualization.
     * @return The graph representation in the specified format.
     */
    fun generateWorkflowGraph(
        workflowId: String,
        steps: List<WorkflowStep>,
        connections: List<StepConnection>,
        format: VisualizationFormat,
        executionMetrics: ExecutionMetrics? = null
    ): String {
        return when (format) {
            VisualizationFormat.DOT -> generateDotGraph(workflowId, steps, connections, executionMetrics)
            VisualizationFormat.MERMAID -> generateMermaidGraph(workflowId, steps, connections, executionMetrics)
            VisualizationFormat.JSON -> generateJsonGraph(workflowId, steps, connections, executionMetrics)
            VisualizationFormat.TEXT -> generateTextGraph(workflowId, steps, connections, executionMetrics)
        }
    }

    private fun generateDotGraph(
        workflowId: String,
        steps: List<WorkflowStep>,
        connections: List<StepConnection>,
        executionMetrics: ExecutionMetrics?
    ): String {
        val sb = StringBuilder()
        sb.appendLine("digraph \"$workflowId\" {")
        sb.appendLine("  rankdir=LR;")
        sb.appendLine("  node [shape=box, style=rounded, fontname=Arial];")
        sb.appendLine("  edge [fontname=Arial];")
        
        // Add nodes
        steps.forEach { step ->
            val color = getStepColor(step.id, executionMetrics)
            val label = getStepLabel(step, executionMetrics)
            sb.appendLine("  \"${step.id}\" [label=\"$label\", fillcolor=\"$color\", style=\"rounded,filled\"];")
        }
        
        // Add edges
        connections.forEach { connection ->
            val label = if (connection.condition != null) " [label=\"${connection.condition}\"]" else ""
            sb.appendLine("  \"${connection.sourceId}\" -> \"${connection.targetId}\"$label;")
        }
        
        sb.appendLine("}")
        return sb.toString()
    }

    private fun generateMermaidGraph(
        workflowId: String,
        steps: List<WorkflowStep>,
        connections: List<StepConnection>,
        executionMetrics: ExecutionMetrics?
    ): String {
        val sb = StringBuilder()
        sb.appendLine("```mermaid")
        sb.appendLine("graph LR")
        sb.appendLine("  %% Workflow: $workflowId")
        
        // Add nodes
        steps.forEach { step ->
            val color = getMermaidStepColor(step.id, executionMetrics)
            val label = getStepLabel(step, executionMetrics)
            sb.appendLine("  ${step.id}[\"$label\"]$color")
        }
        
        // Add edges
        connections.forEach { connection ->
            val label = if (connection.condition != null) "|${connection.condition}|" else ""
            sb.appendLine("  ${connection.sourceId} -->$label ${connection.targetId}")
        }
        
        sb.appendLine("```")
        return sb.toString()
    }

    private fun generateJsonGraph(
        workflowId: String,
        steps: List<WorkflowStep>,
        connections: List<StepConnection>,
        executionMetrics: ExecutionMetrics?
    ): String {
        val nodes = steps.map { step ->
            val stepMetrics = executionMetrics?.stepMetrics?.get(step.id)
            GraphNode(
                id = step.id,
                label = step.name,
                type = step.type,
                status = stepMetrics?.status?.toString() ?: "UNKNOWN",
                data = mapOf(
                    "description" to (step.description ?: ""),
                    "duration" to (stepMetrics?.getDuration()?.toMillis()?.toString() ?: ""),
                    "retryCount" to (stepMetrics?.retryCount?.toString() ?: "0")
                )
            )
        }
        
        val edges = connections.map { connection ->
            GraphEdge(
                source = connection.sourceId,
                target = connection.targetId,
                label = connection.condition ?: "",
                type = connection.type
            )
        }
        
        val graph = Graph(
            id = workflowId,
            nodes = nodes,
            edges = edges,
            metadata = executionMetrics?.let {
                mapOf(
                    "status" to it.status.toString(),
                    "startTime" to it.startTime.toString(),
                    "endTime" to (it.endTime?.toString() ?: ""),
                    "duration" to it.getDuration().toMillis().toString(),
                    "progress" to it.getProgress().toString()
                )
            } ?: emptyMap()
        )
        
        return Json.encodeToString(graph)
    }

    private fun generateTextGraph(
        workflowId: String,
        steps: List<WorkflowStep>,
        connections: List<StepConnection>,
        executionMetrics: ExecutionMetrics?
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Workflow: $workflowId")
        sb.appendLine()
        
        // Add workflow status if available
        executionMetrics?.let {
            sb.appendLine("Status: ${it.status}")
            sb.appendLine("Start Time: ${it.startTime}")
            it.endTime?.let { endTime -> sb.appendLine("End Time: $endTime") }
            sb.appendLine("Duration: ${it.getDuration().toMillis()} ms")
            sb.appendLine("Progress: ${it.getProgress()}%")
            sb.appendLine()
        }
        
        // Add steps
        sb.appendLine("Steps:")
        steps.forEach { step ->
            val stepMetrics = executionMetrics?.stepMetrics?.get(step.id)
            sb.appendLine("  - ${step.id}: ${step.name} (${step.type})")
            step.description?.let { sb.appendLine("    Description: $it") }
            stepMetrics?.let {
                sb.appendLine("    Status: ${it.status}")
                sb.appendLine("    Duration: ${it.getDuration().toMillis()} ms")
                if (it.retryCount > 0) {
                    sb.appendLine("    Retry Count: ${it.retryCount}")
                }
                it.errorMessage?.let { error -> sb.appendLine("    Error: $error") }
            }
        }
        
        sb.appendLine()
        
        // Add connections
        sb.appendLine("Connections:")
        connections.forEach { connection ->
            val conditionText = connection.condition?.let { " [Condition: $it]" } ?: ""
            sb.appendLine("  - ${connection.sourceId} -> ${connection.targetId}$conditionText")
        }
        
        return sb.toString()
    }

    private fun getStepColor(stepId: String, executionMetrics: ExecutionMetrics?): String {
        val stepMetrics = executionMetrics?.stepMetrics?.get(stepId) ?: return "#FFFFFF"
        
        return when (stepMetrics.status) {
            StepStatus.COMPLETED -> "#90EE90" // Light green
            StepStatus.FAILED -> "#FFA07A"    // Light salmon
            StepStatus.RUNNING -> "#ADD8E6"   // Light blue
            StepStatus.SUSPENDED -> "#FFFF99" // Light yellow
            StepStatus.WAITING -> "#D3D3D3"   // Light gray
            StepStatus.SKIPPED -> "#E6E6FA"   // Lavender
        }
    }

    private fun getMermaidStepColor(stepId: String, executionMetrics: ExecutionMetrics?): String {
        val stepMetrics = executionMetrics?.stepMetrics?.get(stepId) ?: return ""
        
        return when (stepMetrics.status) {
            StepStatus.COMPLETED -> ":::completed"
            StepStatus.FAILED -> ":::failed"
            StepStatus.RUNNING -> ":::running"
            StepStatus.SUSPENDED -> ":::suspended"
            StepStatus.WAITING -> ":::waiting"
            StepStatus.SKIPPED -> ":::skipped"
        }
    }

    private fun getStepLabel(step: WorkflowStep, executionMetrics: ExecutionMetrics?): String {
        val stepMetrics = executionMetrics?.stepMetrics?.get(step.id)
        
        val label = StringBuilder("${step.name} (${step.type})")
        
        stepMetrics?.let {
            label.append("\\nStatus: ${it.status}")
            if (it.endTime != null) {
                label.append("\\nDuration: ${it.getDuration().toMillis()} ms")
            }
            if (it.retryCount > 0) {
                label.append("\\nRetries: ${it.retryCount}")
            }
        }
        
        return label.toString()
    }
}

/**
 * Represents a step in a workflow.
 *
 * @property id The ID of the step.
 * @property name The name of the step.
 * @property type The type of the step.
 * @property description An optional description of the step.
 */
data class WorkflowStep(
    val id: String,
    val name: String,
    val type: String,
    val description: String? = null
)

/**
 * Represents a connection between steps in a workflow.
 *
 * @property sourceId The ID of the source step.
 * @property targetId The ID of the target step.
 * @property condition An optional condition for the connection.
 * @property type The type of the connection.
 */
data class StepConnection(
    val sourceId: String,
    val targetId: String,
    val condition: String? = null,
    val type: String = "default"
)

@Serializable
data class Graph(
    val id: String,
    val nodes: List<GraphNode>,
    val edges: List<GraphEdge>,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class GraphNode(
    val id: String,
    val label: String,
    val type: String,
    val status: String,
    val data: Map<String, String> = emptyMap()
)

@Serializable
data class GraphEdge(
    val source: String,
    val target: String,
    val label: String = "",
    val type: String = "default"
)
