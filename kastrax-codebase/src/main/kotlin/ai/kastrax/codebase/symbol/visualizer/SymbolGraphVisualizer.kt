package ai.kastrax.codebase.symbol.visualizer

import ai.kastrax.codebase.symbol.model.SymbolNode
import ai.kastrax.codebase.symbol.model.SymbolRelation
import ai.kastrax.codebase.symbol.model.SymbolRelationGraph
import ai.kastrax.codebase.symbol.model.SymbolRelationType
import ai.kastrax.codebase.symbol.model.SymbolType
import java.io.File

/**
 * 符号图可视化器
 *
 * 用于将符号关系图可视化为DOT格式或HTML格式
 */
class SymbolGraphVisualizer {
    /**
     * 将符号关系图导出为DOT格式
     *
     * @param graph 符号关系图
     * @param outputPath 输出路径
     * @param includeNodeTypes 包含的节点类型
     * @param includeRelationTypes 包含的关系类型
     */
    fun exportToDot(
        graph: SymbolRelationGraph,
        outputPath: String,
        includeNodeTypes: Set<SymbolType> = SymbolType.values().toSet(),
        includeRelationTypes: Set<SymbolRelationType> = SymbolRelationType.values().toSet()
    ) {
        val nodes = graph.getAllNodes().filter { it.type in includeNodeTypes }
        val relations = graph.getAllRelations().filter { it.type in includeRelationTypes }

        val dot = buildString {
            appendLine("digraph G {")
            appendLine("  rankdir=LR;")
            appendLine("  node [shape=box, style=filled, fillcolor=lightblue];")

            // 添加节点
            nodes.forEach { node ->
                val label = "${node.name} (${node.type})"
                appendLine("  \"${node.id}\" [label=\"$label\"];")
            }

            // 添加边
            relations.forEach { relation ->
                val sourceNode = graph.getNodeById(relation.sourceId)
                val targetNode = graph.getNodeById(relation.targetId)

                if (sourceNode != null && targetNode != null) {
                    val label = relation.type.toString()
                    appendLine("  \"${relation.sourceId}\" -> \"${relation.targetId}\" [label=\"$label\"];")
                }
            }

            appendLine("}")
        }

        File(outputPath).writeText(dot)
    }

    /**
     * 将符号关系图导出为HTML格式
     *
     * @param graph 符号关系图
     * @param outputPath 输出路径
     * @param includeNodeTypes 包含的节点类型
     * @param includeRelationTypes 包含的关系类型
     */
    fun exportToHtml(
        graph: SymbolRelationGraph,
        outputPath: String,
        includeNodeTypes: Set<SymbolType> = SymbolType.values().toSet(),
        includeRelationTypes: Set<SymbolRelationType> = SymbolRelationType.values().toSet()
    ) {
        val nodes = graph.getAllNodes().filter { it.type in includeNodeTypes }
        val relations = graph.getAllRelations().filter { it.type in includeRelationTypes }

        val html = buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html>")
            appendLine("<head>")
            appendLine("  <title>Symbol Relation Graph</title>")
            appendLine("  <style>")
            appendLine("    body { font-family: Arial, sans-serif; }")
            appendLine("    .node { margin: 10px; padding: 10px; border: 1px solid #ccc; border-radius: 5px; }")
            appendLine("    .relation { margin: 5px; padding: 5px; border: 1px solid #eee; border-radius: 3px; }")
            appendLine("    .node-type { font-weight: bold; color: #007bff; }")
            appendLine("    .relation-type { font-weight: bold; color: #28a745; }")
            appendLine("  </style>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine("  <h1>Symbol Relation Graph</h1>")

            // 添加节点
            appendLine("  <h2>Nodes</h2>")
            nodes.forEach { node ->
                appendLine("  <div class=\"node\">")
                appendLine("    <div><span class=\"node-type\">${node.type}</span>: ${node.name}</div>")
                appendLine("    <div>ID: ${node.id}</div>")
                appendLine("    <div>Qualified Name: ${node.qualifiedName}</div>")
                appendLine("    <div>Metadata:</div>")
                appendLine("    <ul>")
                node.metadata.forEach { (key, value) ->
                    appendLine("      <li>$key: $value</li>")
                }
                appendLine("    </ul>")
                appendLine("  </div>")
            }

            // 添加关系
            appendLine("  <h2>Relations</h2>")
            relations.forEach { relation ->
                val sourceNode = graph.getNodeById(relation.sourceId)
                val targetNode = graph.getNodeById(relation.targetId)

                if (sourceNode != null && targetNode != null) {
                    appendLine("  <div class=\"relation\">")
                    appendLine("    <div><span class=\"relation-type\">${relation.type}</span>: ${sourceNode.name} -> ${targetNode.name}</div>")
                    appendLine("    <div>ID: ${relation.id}</div>")
                    appendLine("    <div>Source ID: ${relation.sourceId}</div>")
                    appendLine("    <div>Target ID: ${relation.targetId}</div>")
                    appendLine("    <div>Metadata:</div>")
                    appendLine("    <ul>")
                    relation.metadata.forEach { (key, value) ->
                        appendLine("      <li>$key: $value</li>")
                    }
                    appendLine("    </ul>")
                    appendLine("  </div>")
                }
            }

            appendLine("</body>")
            appendLine("</html>")
        }

        File(outputPath).writeText(html)
    }

    /**
     * 将符号关系图导出为JSON格式
     *
     * @param graph 符号关系图
     * @param outputPath 输出路径
     * @param includeNodeTypes 包含的节点类型
     * @param includeRelationTypes 包含的关系类型
     */
    fun exportToJson(
        graph: SymbolRelationGraph,
        outputPath: String,
        includeNodeTypes: Set<SymbolType> = SymbolType.values().toSet(),
        includeRelationTypes: Set<SymbolRelationType> = SymbolRelationType.values().toSet()
    ) {
        val nodes = graph.getAllNodes().filter { it.type in includeNodeTypes }
        val relations = graph.getAllRelations().filter { it.type in includeRelationTypes }

        val json = buildString {
            appendLine("{")
            appendLine("  \"nodes\": [")
            nodes.forEachIndexed { index, node ->
                appendLine("    {")
                appendLine("      \"id\": \"${node.id}\",")
                appendLine("      \"name\": \"${node.name}\",")
                appendLine("      \"qualifiedName\": \"${node.qualifiedName}\",")
                appendLine("      \"type\": \"${node.type}\",")
                appendLine("      \"metadata\": {")
                node.metadata.entries.forEachIndexed { metaIndex, (key, value) ->
                    val comma = if (metaIndex < node.metadata.size - 1) "," else ""
                    appendLine("        \"$key\": \"$value\"$comma")
                }
                appendLine("      }")
                val comma = if (index < nodes.size - 1) "," else ""
                appendLine("    }$comma")
            }
            appendLine("  ],")
            appendLine("  \"relations\": [")
            relations.forEachIndexed { index, relation ->
                appendLine("    {")
                appendLine("      \"id\": \"${relation.id}\",")
                appendLine("      \"sourceId\": \"${relation.sourceId}\",")
                appendLine("      \"targetId\": \"${relation.targetId}\",")
                appendLine("      \"type\": \"${relation.type}\",")
                appendLine("      \"metadata\": {")
                relation.metadata.entries.forEachIndexed { metaIndex, (key, value) ->
                    val comma = if (metaIndex < relation.metadata.size - 1) "," else ""
                    appendLine("        \"$key\": \"$value\"$comma")
                }
                appendLine("      }")
                val comma = if (index < relations.size - 1) "," else ""
                appendLine("    }$comma")
            }
            appendLine("  ]")
            appendLine("}")
        }

        File(outputPath).writeText(json)
    }

    /**
     * 生成节点的详细信息
     *
     * @param node 符号节点
     * @param graph 符号关系图
     * @return 节点详细信息
     */
    fun generateNodeDetails(node: SymbolNode, graph: SymbolRelationGraph): String {
        return buildString {
            appendLine("Node Details:")
            appendLine("  ID: ${node.id}")
            appendLine("  Name: ${node.name}")
            appendLine("  Qualified Name: ${node.qualifiedName}")
            appendLine("  Type: ${node.type}")
            appendLine("  Metadata:")
            node.metadata.forEach { (key, value) ->
                appendLine("    $key: $value")
            }

            // 获取相关节点
            val outgoingEdges = graph.getOutgoingEdges(node.id)
            val incomingEdges = graph.getIncomingEdges(node.id)

            appendLine("  Outgoing Relations:")
            outgoingEdges.forEach { relation ->
                val targetNode = graph.getNodeById(relation.targetId)
                if (targetNode != null) {
                    appendLine("    ${relation.type}: ${targetNode.name} (${targetNode.type})")
                }
            }

            appendLine("  Incoming Relations:")
            incomingEdges.forEach { relation ->
                val sourceNode = graph.getNodeById(relation.sourceId)
                if (sourceNode != null) {
                    appendLine("    ${relation.type}: ${sourceNode.name} (${sourceNode.type})")
                }
            }
        }
    }

    /**
     * 生成关系的详细信息
     *
     * @param relation 符号关系
     * @param graph 符号关系图
     * @return 关系详细信息
     */
    fun generateRelationDetails(relation: SymbolRelation, graph: SymbolRelationGraph): String {
        val sourceNode = graph.getNodeById(relation.sourceId)
        val targetNode = graph.getNodeById(relation.targetId)

        return buildString {
            appendLine("Relation Details:")
            appendLine("  ID: ${relation.id}")
            appendLine("  Type: ${relation.type}")
            appendLine("  Source: ${sourceNode?.name ?: relation.sourceId} (${sourceNode?.type ?: "Unknown"})")
            appendLine("  Target: ${targetNode?.name ?: relation.targetId} (${targetNode?.type ?: "Unknown"})")
            appendLine("  Metadata:")
            relation.metadata.forEach { (key, value) ->
                appendLine("    $key: $value")
            }
        }
    }
}
