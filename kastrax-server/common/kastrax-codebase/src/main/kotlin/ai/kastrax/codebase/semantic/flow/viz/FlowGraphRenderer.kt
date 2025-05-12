package ai.kastrax.codebase.semantic.flow.viz

import ai.kastrax.codebase.semantic.flow.FlowEdgeType
import ai.kastrax.codebase.semantic.flow.FlowGraph
import ai.kastrax.codebase.semantic.flow.FlowNodeType
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

/**
 * 流图渲染器配置
 *
 * @property outputFormat 输出格式
 * @property includeMetadata 是否包含元数据
 * @property includeNodeLabels 是否包含节点标签
 * @property includeEdgeLabels 是否包含边标签
 * @property maxLabelLength 最大标签长度
 */
data class FlowGraphRendererConfig(
    val outputFormat: OutputFormat = OutputFormat.DOT,
    val includeMetadata: Boolean = false,
    val includeNodeLabels: Boolean = true,
    val includeEdgeLabels: Boolean = true,
    val maxLabelLength: Int = 50
)

/**
 * 输出格式
 */
enum class OutputFormat {
    DOT,
    SVG,
    PNG,
    JSON
}

/**
 * 流图渲染器
 *
 * 将流图渲染为不同格式的可视化表示
 *
 * @property config 配置
 */
class FlowGraphRenderer(
    private val config: FlowGraphRendererConfig = FlowGraphRendererConfig()
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 渲染流图
     *
     * @param graph 流图
     * @return 渲染结果
     */
    fun render(graph: FlowGraph): String {
        return when (config.outputFormat) {
            OutputFormat.DOT -> renderDot(graph)
            OutputFormat.JSON -> renderJson(graph)
            OutputFormat.SVG, OutputFormat.PNG -> {
                logger.warn { "直接渲染为 ${config.outputFormat} 暂不支持，请先渲染为 DOT 格式，然后使用 Graphviz 转换" }
                renderDot(graph)
            }
        }
    }

    /**
     * 渲染流图并保存到文件
     *
     * @param graph 流图
     * @param filePath 文件路径
     * @return 是否成功
     */
    fun renderToFile(graph: FlowGraph, filePath: String): Boolean {
        try {
            val content = render(graph)
            File(filePath).writeText(content)
            return true
        } catch (e: Exception) {
            logger.error(e) { "渲染流图到文件时出错: $filePath" }
            return false
        }
    }

    /**
     * 渲染为 DOT 格式
     *
     * @param graph 流图
     * @return DOT 格式字符串
     */
    private fun renderDot(graph: FlowGraph): String {
        val sb = StringBuilder()
        
        // 图头
        sb.appendLine("digraph \"${escapeString(graph.name)}\" {")
        sb.appendLine("  // 图属性")
        sb.appendLine("  graph [rankdir=TB, fontname=\"Arial\", fontsize=12, dpi=300];")
        sb.appendLine("  node [shape=box, style=filled, fontname=\"Arial\", fontsize=10];")
        sb.appendLine("  edge [fontname=\"Arial\", fontsize=8];")
        sb.appendLine()
        
        // 图元数据
        if (config.includeMetadata) {
            sb.appendLine("  // 图元数据")
            sb.appendLine("  graph [")
            sb.appendLine("    label=\"${escapeString(graph.name)}\",")
            graph.metadata.forEach { (key, value) ->
                sb.appendLine("    ${key}=\"${escapeString(value.toString())}\",")
            }
            sb.appendLine("  ];")
            sb.appendLine()
        }
        
        // 节点
        sb.appendLine("  // 节点")
        graph.nodes.values.forEach { node ->
            val label = if (config.includeNodeLabels) {
                val nodeLabel = node.metadata["label"]?.toString() ?: node.element?.name ?: node.id
                truncateLabel(nodeLabel)
            } else {
                node.id
            }
            
            val shape = when (node.type) {
                FlowNodeType.ENTRY -> "ellipse"
                FlowNodeType.EXIT -> "ellipse"
                FlowNodeType.CONDITION -> "diamond"
                FlowNodeType.LOOP -> "trapezium"
                FlowNodeType.CALL -> "cds"
                FlowNodeType.RETURN -> "invtrapezium"
                FlowNodeType.THROW -> "invhouse"
                FlowNodeType.CATCH -> "house"
                FlowNodeType.FINALLY -> "pentagon"
                else -> "box"
            }
            
            val color = when (node.type) {
                FlowNodeType.ENTRY -> "green"
                FlowNodeType.EXIT -> "red"
                FlowNodeType.CONDITION -> "yellow"
                FlowNodeType.LOOP -> "orange"
                FlowNodeType.CALL -> "lightblue"
                FlowNodeType.RETURN -> "pink"
                FlowNodeType.THROW -> "crimson"
                FlowNodeType.CATCH -> "purple"
                FlowNodeType.FINALLY -> "magenta"
                FlowNodeType.ASSIGNMENT -> "cyan"
                FlowNodeType.DECLARATION -> "skyblue"
                FlowNodeType.REFERENCE -> "lightgrey"
                else -> "white"
            }
            
            sb.append("  \"${node.id}\" [")
            sb.append("label=\"${escapeString(label)}\", ")
            sb.append("shape=$shape, ")
            sb.append("fillcolor=\"$color\"")
            
            if (config.includeMetadata) {
                node.metadata.forEach { (key, value) ->
                    if (key != "label") {
                        sb.append(", ${key}=\"${escapeString(value.toString())}\"")
                    }
                }
            }
            
            sb.appendLine("];")
        }
        sb.appendLine()
        
        // 边
        sb.appendLine("  // 边")
        graph.edges.values.forEach { edge ->
            val label = if (config.includeEdgeLabels) {
                val edgeLabel = edge.metadata["label"]?.toString() ?: edge.type.name
                truncateLabel(edgeLabel)
            } else {
                ""
            }
            
            val style = when (edge.type) {
                FlowEdgeType.SEQUENTIAL -> "solid"
                FlowEdgeType.CONDITIONAL_TRUE -> "solid"
                FlowEdgeType.CONDITIONAL_FALSE -> "dashed"
                FlowEdgeType.LOOP_BACK -> "dotted"
                FlowEdgeType.EXCEPTION -> "bold"
                FlowEdgeType.CALL -> "solid"
                FlowEdgeType.RETURN -> "dashed"
                FlowEdgeType.DATA_DEPENDENCY -> "dotted"
                FlowEdgeType.CONTROL_DEPENDENCY -> "dashed"
                else -> "solid"
            }
            
            val color = when (edge.type) {
                FlowEdgeType.SEQUENTIAL -> "black"
                FlowEdgeType.CONDITIONAL_TRUE -> "green"
                FlowEdgeType.CONDITIONAL_FALSE -> "red"
                FlowEdgeType.LOOP_BACK -> "orange"
                FlowEdgeType.EXCEPTION -> "red"
                FlowEdgeType.CALL -> "blue"
                FlowEdgeType.RETURN -> "purple"
                FlowEdgeType.DATA_DEPENDENCY -> "darkgreen"
                FlowEdgeType.CONTROL_DEPENDENCY -> "darkblue"
                else -> "black"
            }
            
            sb.append("  \"${edge.sourceId}\" -> \"${edge.targetId}\" [")
            if (label.isNotEmpty()) {
                sb.append("label=\"${escapeString(label)}\", ")
            }
            sb.append("style=$style, ")
            sb.append("color=\"$color\"")
            
            if (config.includeMetadata) {
                edge.metadata.forEach { (key, value) ->
                    if (key != "label") {
                        sb.append(", ${key}=\"${escapeString(value.toString())}\"")
                    }
                }
            }
            
            sb.appendLine("];")
        }
        
        // 图尾
        sb.appendLine("}")
        
        return sb.toString()
    }

    /**
     * 渲染为 JSON 格式
     *
     * @param graph 流图
     * @return JSON 格式字符串
     */
    private fun renderJson(graph: FlowGraph): String {
        val sb = StringBuilder()
        
        sb.appendLine("{")
        sb.appendLine("  \"id\": \"${escapeJsonString(graph.id)}\",")
        sb.appendLine("  \"name\": \"${escapeJsonString(graph.name)}\",")
        sb.appendLine("  \"type\": \"${graph.type}\",")
        
        // 元数据
        sb.appendLine("  \"metadata\": {")
        graph.metadata.entries.forEachIndexed { index, (key, value) ->
            sb.append("    \"${escapeJsonString(key)}\": \"${escapeJsonString(value.toString())}\"")
            if (index < graph.metadata.size - 1) {
                sb.appendLine(",")
            } else {
                sb.appendLine()
            }
        }
        sb.appendLine("  },")
        
        // 节点
        sb.appendLine("  \"nodes\": [")
        graph.nodes.values.forEachIndexed { index, node ->
            sb.appendLine("    {")
            sb.appendLine("      \"id\": \"${escapeJsonString(node.id)}\",")
            sb.appendLine("      \"type\": \"${node.type}\",")
            
            // 节点元数据
            sb.appendLine("      \"metadata\": {")
            node.metadata.entries.forEachIndexed { metaIndex, (key, value) ->
                sb.append("        \"${escapeJsonString(key)}\": \"${escapeJsonString(value.toString())}\"")
                if (metaIndex < node.metadata.size - 1) {
                    sb.appendLine(",")
                } else {
                    sb.appendLine()
                }
            }
            sb.appendLine("      },")
            
            // 关联的代码元素
            sb.appendLine("      \"element\": ${if (node.element != null) "\"${escapeJsonString(node.element.id)}\"" else "null"}")
            
            sb.append("    }")
            if (index < graph.nodes.size - 1) {
                sb.appendLine(",")
            } else {
                sb.appendLine()
            }
        }
        sb.appendLine("  ],")
        
        // 边
        sb.appendLine("  \"edges\": [")
        graph.edges.values.forEachIndexed { index, edge ->
            sb.appendLine("    {")
            sb.appendLine("      \"id\": \"${escapeJsonString(edge.id)}\",")
            sb.appendLine("      \"sourceId\": \"${escapeJsonString(edge.sourceId)}\",")
            sb.appendLine("      \"targetId\": \"${escapeJsonString(edge.targetId)}\",")
            sb.appendLine("      \"type\": \"${edge.type}\",")
            
            // 边元数据
            sb.appendLine("      \"metadata\": {")
            edge.metadata.entries.forEachIndexed { metaIndex, (key, value) ->
                sb.append("        \"${escapeJsonString(key)}\": \"${escapeJsonString(value.toString())}\"")
                if (metaIndex < edge.metadata.size - 1) {
                    sb.appendLine(",")
                } else {
                    sb.appendLine()
                }
            }
            sb.appendLine("      }")
            
            sb.append("    }")
            if (index < graph.edges.size - 1) {
                sb.appendLine(",")
            } else {
                sb.appendLine()
            }
        }
        sb.appendLine("  ],")
        
        // 入口和出口节点
        sb.appendLine("  \"entryNodeId\": ${if (graph.entryNodeId != null) "\"${escapeJsonString(graph.entryNodeId!!)}\"" else "null"},")
        sb.appendLine("  \"exitNodeIds\": [")
        graph.exitNodeIds.forEachIndexed { index, nodeId ->
            sb.append("    \"${escapeJsonString(nodeId)}\"")
            if (index < graph.exitNodeIds.size - 1) {
                sb.appendLine(",")
            } else {
                sb.appendLine()
            }
        }
        sb.appendLine("  ]")
        
        sb.appendLine("}")
        
        return sb.toString()
    }

    /**
     * 截断标签
     *
     * @param label 标签
     * @return 截断后的标签
     */
    private fun truncateLabel(label: String): String {
        return if (label.length > config.maxLabelLength) {
            label.substring(0, config.maxLabelLength - 3) + "..."
        } else {
            label
        }
    }

    /**
     * 转义字符串（DOT 格式）
     *
     * @param str 字符串
     * @return 转义后的字符串
     */
    private fun escapeString(str: String): String {
        return str.replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * 转义字符串（JSON 格式）
     *
     * @param str 字符串
     * @return 转义后的字符串
     */
    private fun escapeJsonString(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
    }
}
