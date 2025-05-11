package ai.kastrax.codebase.symbol.visualization

import ai.kastrax.codebase.symbol.model.SymbolGraph
import ai.kastrax.codebase.symbol.model.SymbolNode
import ai.kastrax.codebase.symbol.model.SymbolRelation
import ai.kastrax.codebase.symbol.model.SymbolRelationType
import ai.kastrax.codebase.symbol.model.SymbolType
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path

private val logger = KotlinLogging.logger {}

/**
 * 符号图可视化器配置
 *
 * @property maxNodes 最大节点数
 * @property includeNodeTypes 包含的节点类型
 * @property includeRelationTypes 包含的关系类型
 * @property outputFormat 输出格式
 */
data class SymbolGraphVisualizerConfig(
    val maxNodes: Int = 100,
    val includeNodeTypes: Set<SymbolType>? = null,
    val includeRelationTypes: Set<SymbolRelationType>? = null,
    val outputFormat: OutputFormat = OutputFormat.DOT
)

/**
 * 输出格式
 */
enum class OutputFormat {
    DOT,
    JSON,
    HTML
}

/**
 * 符号图可视化器
 *
 * 用于可视化符号关系图
 *
 * @property config 配置
 */
class SymbolGraphVisualizer(
    private val config: SymbolGraphVisualizerConfig = SymbolGraphVisualizerConfig()
) {
    /**
     * 可视化符号图
     *
     * @param graph 符号图
     * @param outputPath 输出路径
     * @param filter 节点过滤器
     * @return 是否成功
     */
    fun visualize(
        graph: SymbolGraph,
        outputPath: Path,
        filter: (SymbolNode) -> Boolean = { true }
    ): Boolean {
        try {
            logger.info { "开始可视化符号图: ${graph.name}" }
            
            // 获取节点和关系
            val nodes = graph.getAllNodes()
                .filter(filter)
                .filter { config.includeNodeTypes == null || config.includeNodeTypes.contains(it.type) }
                .take(config.maxNodes)
            
            val nodeIds = nodes.map { it.id }.toSet()
            
            val relations = graph.getAllRelations()
                .filter { nodeIds.contains(it.sourceId) && nodeIds.contains(it.targetId) }
                .filter { config.includeRelationTypes == null || config.includeRelationTypes.contains(it.type) }
            
            // 根据输出格式生成可视化
            val content = when (config.outputFormat) {
                OutputFormat.DOT -> generateDotGraph(graph.name, nodes, relations)
                OutputFormat.JSON -> generateJsonGraph(graph.name, nodes, relations)
                OutputFormat.HTML -> generateHtmlGraph(graph.name, nodes, relations)
            }
            
            // 写入文件
            File(outputPath.toString()).writeText(content)
            
            logger.info { "符号图可视化完成: $outputPath" }
            
            return true
        } catch (e: Exception) {
            logger.error(e) { "符号图可视化失败: ${e.message}" }
            return false
        }
    }
    
    /**
     * 生成 DOT 格式的图
     *
     * @param name 图名称
     * @param nodes 节点列表
     * @param relations 关系列表
     * @return DOT 格式的图
     */
    private fun generateDotGraph(
        name: String,
        nodes: List<SymbolNode>,
        relations: List<SymbolRelation>
    ): String {
        val sb = StringBuilder()
        
        sb.appendLine("digraph \"$name\" {")
        sb.appendLine("  rankdir=LR;")
        sb.appendLine("  node [shape=box, style=filled, fontname=\"Arial\"];")
        sb.appendLine("  edge [fontname=\"Arial\"];")
        
        // 添加节点
        nodes.forEach { node ->
            val color = getNodeColor(node.type)
            val label = escapeLabel("${node.name}\\n(${node.type.name.lowercase()})")
            
            sb.appendLine("  \"${node.id}\" [label=\"$label\", fillcolor=\"$color\"];")
        }
        
        // 添加关系
        relations.forEach { relation ->
            val label = relation.type.name.lowercase()
            val style = getEdgeStyle(relation.type)
            
            sb.appendLine("  \"${relation.sourceId}\" -> \"${relation.targetId}\" [label=\"$label\", $style];")
        }
        
        sb.appendLine("}")
        
        return sb.toString()
    }
    
    /**
     * 生成 JSON 格式的图
     *
     * @param name 图名称
     * @param nodes 节点列表
     * @param relations 关系列表
     * @return JSON 格式的图
     */
    private fun generateJsonGraph(
        name: String,
        nodes: List<SymbolNode>,
        relations: List<SymbolRelation>
    ): String {
        val sb = StringBuilder()
        
        sb.appendLine("{")
        sb.appendLine("  \"name\": \"$name\",")
        sb.appendLine("  \"nodes\": [")
        
        // 添加节点
        nodes.forEachIndexed { index, node ->
            sb.appendLine("    {")
            sb.appendLine("      \"id\": \"${node.id}\",")
            sb.appendLine("      \"name\": \"${escapeJson(node.name)}\",")
            sb.appendLine("      \"qualifiedName\": \"${escapeJson(node.qualifiedName)}\",")
            sb.appendLine("      \"type\": \"${node.type.name.lowercase()}\",")
            sb.appendLine("      \"kind\": \"${node.kind.name.lowercase()}\",")
            sb.appendLine("      \"visibility\": \"${node.visibility.name.lowercase()}\"")
            sb.append("    }")
            
            if (index < nodes.size - 1) {
                sb.appendLine(",")
            } else {
                sb.appendLine("")
            }
        }
        
        sb.appendLine("  ],")
        sb.appendLine("  \"relations\": [")
        
        // 添加关系
        relations.forEachIndexed { index, relation ->
            sb.appendLine("    {")
            sb.appendLine("      \"id\": \"${relation.id}\",")
            sb.appendLine("      \"sourceId\": \"${relation.sourceId}\",")
            sb.appendLine("      \"targetId\": \"${relation.targetId}\",")
            sb.appendLine("      \"type\": \"${relation.type.name.lowercase()}\"")
            sb.append("    }")
            
            if (index < relations.size - 1) {
                sb.appendLine(",")
            } else {
                sb.appendLine("")
            }
        }
        
        sb.appendLine("  ]")
        sb.appendLine("}")
        
        return sb.toString()
    }
    
    /**
     * 生成 HTML 格式的图
     *
     * @param name 图名称
     * @param nodes 节点列表
     * @param relations 关系列表
     * @return HTML 格式的图
     */
    private fun generateHtmlGraph(
        name: String,
        nodes: List<SymbolNode>,
        relations: List<SymbolRelation>
    ): String {
        val sb = StringBuilder()
        
        sb.appendLine("<!DOCTYPE html>")
        sb.appendLine("<html>")
        sb.appendLine("<head>")
        sb.appendLine("  <title>Symbol Graph: $name</title>")
        sb.appendLine("  <style>")
        sb.appendLine("    body { font-family: Arial, sans-serif; margin: 20px; }")
        sb.appendLine("    h1 { color: #333; }")
        sb.appendLine("    .graph { margin-top: 20px; }")
        sb.appendLine("    .node { padding: 10px; margin: 5px; border-radius: 5px; display: inline-block; }")
        sb.appendLine("    .relation { margin: 10px 0; }")
        sb.appendLine("    .class { background-color: #AED6F1; }")
        sb.appendLine("    .interface { background-color: #D5F5E3; }")
        sb.appendLine("    .method { background-color: #F9E79F; }")
        sb.appendLine("    .field { background-color: #F5CBA7; }")
        sb.appendLine("    .package { background-color: #D2B4DE; }")
        sb.appendLine("    .unknown { background-color: #CACFD2; }")
        sb.appendLine("  </style>")
        sb.appendLine("  <script src=\"https://d3js.org/d3.v7.min.js\"></script>")
        sb.appendLine("</head>")
        sb.appendLine("<body>")
        sb.appendLine("  <h1>Symbol Graph: $name</h1>")
        
        // 添加节点信息
        sb.appendLine("  <h2>Nodes (${nodes.size})</h2>")
        sb.appendLine("  <div class=\"graph\">")
        
        nodes.forEach { node ->
            val cssClass = when (node.type) {
                SymbolType.CLASS -> "class"
                SymbolType.INTERFACE -> "interface"
                SymbolType.METHOD, SymbolType.CONSTRUCTOR -> "method"
                SymbolType.FIELD, SymbolType.PROPERTY -> "field"
                SymbolType.PACKAGE, SymbolType.NAMESPACE -> "package"
                else -> "unknown"
            }
            
            sb.appendLine("    <div class=\"node $cssClass\">")
            sb.appendLine("      <div><strong>${escapeHtml(node.name)}</strong></div>")
            sb.appendLine("      <div><small>${node.type.name.lowercase()}</small></div>")
            sb.appendLine("    </div>")
        }
        
        sb.appendLine("  </div>")
        
        // 添加关系信息
        sb.appendLine("  <h2>Relations (${relations.size})</h2>")
        sb.appendLine("  <div>")
        
        relations.take(100).forEach { relation ->
            val sourceNode = nodes.find { it.id == relation.sourceId }
            val targetNode = nodes.find { it.id == relation.targetId }
            
            if (sourceNode != null && targetNode != null) {
                sb.appendLine("    <div class=\"relation\">")
                sb.appendLine("      <strong>${escapeHtml(sourceNode.name)}</strong> ")
                sb.appendLine("      <span>${relation.type.name.lowercase()}</span> ")
                sb.appendLine("      <strong>${escapeHtml(targetNode.name)}</strong>")
                sb.appendLine("    </div>")
            }
        }
        
        sb.appendLine("  </div>")
        
        // 添加 D3.js 可视化
        sb.appendLine("  <h2>Interactive Graph</h2>")
        sb.appendLine("  <div id=\"graph-container\" style=\"width: 100%; height: 600px; border: 1px solid #ccc;\"></div>")
        
        sb.appendLine("  <script>")
        sb.appendLine("    const data = {")
        sb.appendLine("      nodes: [")
        
        nodes.forEachIndexed { index, node ->
            sb.append("        { id: \"${node.id}\", name: \"${escapeJs(node.name)}\", type: \"${node.type.name.lowercase()}\" }")
            
            if (index < nodes.size - 1) {
                sb.appendLine(",")
            } else {
                sb.appendLine("")
            }
        }
        
        sb.appendLine("      ],")
        sb.appendLine("      links: [")
        
        relations.forEachIndexed { index, relation ->
            sb.append("        { source: \"${relation.sourceId}\", target: \"${relation.targetId}\", type: \"${relation.type.name.lowercase()}\" }")
            
            if (index < relations.size - 1) {
                sb.appendLine(",")
            } else {
                sb.appendLine("")
            }
        }
        
        sb.appendLine("      ]")
        sb.appendLine("    };")
        
        sb.appendLine("    const width = document.getElementById('graph-container').clientWidth;")
        sb.appendLine("    const height = document.getElementById('graph-container').clientHeight;")
        
        sb.appendLine("    const color = d3.scaleOrdinal()")
        sb.appendLine("      .domain([\"class\", \"interface\", \"method\", \"field\", \"package\", \"namespace\"])")
        sb.appendLine("      .range([\"#AED6F1\", \"#D5F5E3\", \"#F9E79F\", \"#F5CBA7\", \"#D2B4DE\", \"#CACFD2\"]);")
        
        sb.appendLine("    const simulation = d3.forceSimulation(data.nodes)")
        sb.appendLine("      .force(\"link\", d3.forceLink(data.links).id(d => d.id).distance(100))")
        sb.appendLine("      .force(\"charge\", d3.forceManyBody().strength(-300))")
        sb.appendLine("      .force(\"center\", d3.forceCenter(width / 2, height / 2));")
        
        sb.appendLine("    const svg = d3.select(\"#graph-container\").append(\"svg\")")
        sb.appendLine("      .attr(\"width\", width)")
        sb.appendLine("      .attr(\"height\", height);")
        
        sb.appendLine("    const link = svg.append(\"g\")")
        sb.appendLine("      .selectAll(\"line\")")
        sb.appendLine("      .data(data.links)")
        sb.appendLine("      .enter().append(\"line\")")
        sb.appendLine("      .attr(\"stroke\", \"#999\")")
        sb.appendLine("      .attr(\"stroke-opacity\", 0.6)")
        sb.appendLine("      .attr(\"stroke-width\", 1.5);")
        
        sb.appendLine("    const node = svg.append(\"g\")")
        sb.appendLine("      .selectAll(\"circle\")")
        sb.appendLine("      .data(data.nodes)")
        sb.appendLine("      .enter().append(\"circle\")")
        sb.appendLine("      .attr(\"r\", 8)")
        sb.appendLine("      .attr(\"fill\", d => color(d.type))")
        sb.appendLine("      .call(d3.drag()")
        sb.appendLine("        .on(\"start\", dragstarted)")
        sb.appendLine("        .on(\"drag\", dragged)")
        sb.appendLine("        .on(\"end\", dragended));")
        
        sb.appendLine("    const text = svg.append(\"g\")")
        sb.appendLine("      .selectAll(\"text\")")
        sb.appendLine("      .data(data.nodes)")
        sb.appendLine("      .enter().append(\"text\")")
        sb.appendLine("      .text(d => d.name)")
        sb.appendLine("      .attr(\"font-size\", 10)")
        sb.appendLine("      .attr(\"dx\", 12)")
        sb.appendLine("      .attr(\"dy\", 4);")
        
        sb.appendLine("    node.append(\"title\")")
        sb.appendLine("      .text(d => `\${d.name} (\${d.type})`);")
        
        sb.appendLine("    simulation.on(\"tick\", () => {")
        sb.appendLine("      link")
        sb.appendLine("        .attr(\"x1\", d => d.source.x)")
        sb.appendLine("        .attr(\"y1\", d => d.source.y)")
        sb.appendLine("        .attr(\"x2\", d => d.target.x)")
        sb.appendLine("        .attr(\"y2\", d => d.target.y);")
        
        sb.appendLine("      node")
        sb.appendLine("        .attr(\"cx\", d => d.x)")
        sb.appendLine("        .attr(\"cy\", d => d.y);")
        
        sb.appendLine("      text")
        sb.appendLine("        .attr(\"x\", d => d.x)")
        sb.appendLine("        .attr(\"y\", d => d.y);")
        sb.appendLine("    });")
        
        sb.appendLine("    function dragstarted(event, d) {")
        sb.appendLine("      if (!event.active) simulation.alphaTarget(0.3).restart();")
        sb.appendLine("      d.fx = d.x;")
        sb.appendLine("      d.fy = d.y;")
        sb.appendLine("    }")
        
        sb.appendLine("    function dragged(event, d) {")
        sb.appendLine("      d.fx = event.x;")
        sb.appendLine("      d.fy = event.y;")
        sb.appendLine("    }")
        
        sb.appendLine("    function dragended(event, d) {")
        sb.appendLine("      if (!event.active) simulation.alphaTarget(0);")
        sb.appendLine("      d.fx = null;")
        sb.appendLine("      d.fy = null;")
        sb.appendLine("    }")
        sb.appendLine("  </script>")
        
        sb.appendLine("</body>")
        sb.appendLine("</html>")
        
        return sb.toString()
    }
    
    /**
     * 获取节点颜色
     *
     * @param type 符号类型
     * @return 颜色
     */
    private fun getNodeColor(type: SymbolType): String {
        return when (type) {
            SymbolType.CLASS -> "#AED6F1"
            SymbolType.INTERFACE -> "#D5F5E3"
            SymbolType.ENUM -> "#ABEBC6"
            SymbolType.ANNOTATION -> "#F9E79F"
            SymbolType.METHOD, SymbolType.CONSTRUCTOR -> "#F5CBA7"
            SymbolType.FIELD, SymbolType.PROPERTY -> "#D2B4DE"
            SymbolType.PARAMETER -> "#F7DC6F"
            SymbolType.LOCAL_VARIABLE -> "#F8C471"
            SymbolType.PACKAGE, SymbolType.NAMESPACE -> "#AEB6BF"
            else -> "#CACFD2"
        }
    }
    
    /**
     * 获取边样式
     *
     * @param type 关系类型
     * @return 样式
     */
    private fun getEdgeStyle(type: SymbolRelationType): String {
        return when (type) {
            SymbolRelationType.EXTENDS -> "color=\"#3498DB\", style=solid, penwidth=2"
            SymbolRelationType.IMPLEMENTS -> "color=\"#2ECC71\", style=dashed, penwidth=2"
            SymbolRelationType.REFERENCES -> "color=\"#E74C3C\", style=dotted"
            SymbolRelationType.CALLS -> "color=\"#9B59B6\", style=solid"
            SymbolRelationType.OVERRIDES -> "color=\"#F1C40F\", style=dashed"
            SymbolRelationType.CONTAINS -> "color=\"#95A5A6\", style=solid"
            SymbolRelationType.IMPORTS -> "color=\"#BDC3C7\", style=dotted"
            else -> "color=\"#7F8C8D\", style=solid"
        }
    }
    
    /**
     * 转义 DOT 标签
     *
     * @param label 标签
     * @return 转义后的标签
     */
    private fun escapeLabel(label: String): String {
        return label.replace("\"", "\\\"")
    }
    
    /**
     * 转义 JSON 字符串
     *
     * @param str 字符串
     * @return 转义后的字符串
     */
    private fun escapeJson(str: String): String {
        return str.replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
    
    /**
     * 转义 HTML 字符串
     *
     * @param str 字符串
     * @return 转义后的字符串
     */
    private fun escapeHtml(str: String): String {
        return str.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
    
    /**
     * 转义 JavaScript 字符串
     *
     * @param str 字符串
     * @return 转义后的字符串
     */
    private fun escapeJs(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
