package ai.kastrax.actor.monitoring

import ai.kastrax.actor.network.AgentRelation
import ai.kastrax.actor.network.NetworkTopology
import java.io.File

/**
 * Actor 网络可视化工具，用于创建网络拓扑的可视化表示
 */
class ActorNetworkVisualizer {
    
    /**
     * 生成网络拓扑的 DOT 格式表示
     *
     * @param topology 网络拓扑
     * @param title 图表标题
     * @return DOT 格式的图表描述
     */
    fun generateDotGraph(topology: NetworkTopology, title: String = "Agent Network"): String {
        val nodes = topology.getAllNodes()
        val edges = topology.getAllEdges()
        
        val sb = StringBuilder()
        sb.appendLine("digraph \"$title\" {")
        sb.appendLine("  rankdir=LR;")
        sb.appendLine("  node [shape=box, style=filled, fillcolor=lightblue];")
        sb.appendLine()
        
        // 添加节点
        nodes.forEach { node ->
            sb.appendLine("  \"$node\" [label=\"$node\"];")
        }
        sb.appendLine()
        
        // 添加边
        edges.forEach { (fromId, toId, relation) ->
            val color = getRelationColor(relation)
            val label = getRelationLabel(relation)
            sb.appendLine("  \"$fromId\" -> \"$toId\" [label=\"$label\", color=\"$color\"];")
        }
        
        sb.appendLine("}")
        return sb.toString()
    }
    
    /**
     * 生成网络拓扑的 HTML 可视化
     *
     * @param topology 网络拓扑
     * @param title 图表标题
     * @param includeStats 是否包含网络统计信息
     * @return HTML 格式的可视化
     */
    fun generateHtmlVisualization(
        topology: NetworkTopology,
        title: String = "Agent Network",
        includeStats: Boolean = true
    ): String {
        val nodes = topology.getAllNodes()
        val edges = topology.getAllEdges()
        
        // 准备节点数据
        val nodesJson = nodes.joinToString(",") { node ->
            val connectedNodes = topology.getConnectedNodes(node)
            val group = when {
                connectedNodes.isEmpty() -> 1 // 孤立节点
                topology.getNodesByRelation(node, AgentRelation.MASTER).isNotEmpty() -> 2 // 主节点
                topology.getNodesByRelation(node, AgentRelation.SLAVE).isNotEmpty() -> 3 // 从节点
                topology.getNodesByRelation(node, AgentRelation.SUPERVISOR).isNotEmpty() -> 4 // 监督者
                else -> 5 // 其他节点
            }
            
            """
            {
              "id": "$node",
              "label": "$node",
              "group": $group,
              "connections": ${connectedNodes.size}
            }
            """.trimIndent()
        }
        
        // 准备边数据
        val edgesJson = edges.joinToString(",") { (fromId, toId, relation) ->
            val value = when (relation) {
                AgentRelation.MASTER, AgentRelation.SUPERVISOR -> 3
                AgentRelation.SLAVE, AgentRelation.SUPERVISED -> 2
                else -> 1
            }
            
            """
            {
              "from": "$fromId",
              "to": "$toId",
              "label": "${getRelationLabel(relation)}",
              "value": $value,
              "arrows": "to",
              "color": "${getRelationColor(relation)}"
            }
            """.trimIndent()
        }
        
        // 计算网络统计信息
        val statsHtml = if (includeStats) {
            val nodeCount = nodes.size
            val edgeCount = edges.size
            val density = if (nodeCount > 1) {
                2.0 * edgeCount / (nodeCount * (nodeCount - 1))
            } else {
                0.0
            }
            
            val degreeDistribution = nodes.map { node ->
                topology.getConnectedNodes(node).size
            }.groupingBy { it }.eachCount()
            
            val avgDegree = if (nodeCount > 0) {
                degreeDistribution.entries.sumOf { it.key * it.value }.toDouble() / nodeCount
            } else {
                0.0
            }
            
            """
            <div class="stats-container">
              <h3>Network Statistics</h3>
              <table class="stats-table">
                <tr><td>Nodes:</td><td>$nodeCount</td></tr>
                <tr><td>Edges:</td><td>$edgeCount</td></tr>
                <tr><td>Density:</td><td>${String.format("%.4f", density)}</td></tr>
                <tr><td>Avg. Degree:</td><td>${String.format("%.2f", avgDegree)}</td></tr>
              </table>
              
              <h4>Degree Distribution</h4>
              <div class="degree-distribution">
                ${degreeDistribution.entries.sortedBy { it.key }.joinToString("") { (degree, count) ->
                    """<div class="degree-bar" style="height: ${count * 20}px;" title="Degree $degree: $count nodes"></div>"""
                }}
              </div>
              <div class="degree-labels">
                ${degreeDistribution.keys.sorted().joinToString("") { degree ->
                    """<div class="degree-label">$degree</div>"""
                }}
              </div>
            </div>
            """.trimIndent()
        } else {
            ""
        }
        
        // 生成 HTML
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>$title</title>
            <script type="text/javascript" src="https://unpkg.com/vis-network/standalone/umd/vis-network.min.js"></script>
            <style type="text/css">
                body, html {
                    margin: 0;
                    padding: 0;
                    font-family: Arial, sans-serif;
                    height: 100%;
                }
                #container {
                    display: flex;
                    height: 100%;
                }
                #network {
                    flex: 1;
                    border: 1px solid #ccc;
                }
                #sidebar {
                    width: 300px;
                    padding: 20px;
                    background-color: #f8f8f8;
                    border-left: 1px solid #ccc;
                    overflow: auto;
                }
                h1 {
                    margin-top: 0;
                    color: #333;
                }
                .stats-container {
                    margin-top: 20px;
                }
                .stats-table {
                    width: 100%;
                    border-collapse: collapse;
                }
                .stats-table td {
                    padding: 5px;
                    border-bottom: 1px solid #ddd;
                }
                .stats-table td:first-child {
                    font-weight: bold;
                }
                .degree-distribution {
                    display: flex;
                    align-items: flex-end;
                    height: 100px;
                    margin-top: 10px;
                    border-bottom: 1px solid #333;
                }
                .degree-bar {
                    flex: 1;
                    background-color: #3498db;
                    margin: 0 2px;
                }
                .degree-labels {
                    display: flex;
                    margin-top: 5px;
                }
                .degree-label {
                    flex: 1;
                    text-align: center;
                    font-size: 12px;
                }
                #node-info {
                    margin-top: 20px;
                    padding: 10px;
                    background-color: #fff;
                    border: 1px solid #ddd;
                    border-radius: 4px;
                    display: none;
                }
            </style>
        </head>
        <body>
            <div id="container">
                <div id="network"></div>
                <div id="sidebar">
                    <h1>$title</h1>
                    $statsHtml
                    <div id="node-info"></div>
                </div>
            </div>
            
            <script type="text/javascript">
                // 创建节点和边数据
                var nodes = new vis.DataSet([
                    $nodesJson
                ]);
                
                var edges = new vis.DataSet([
                    $edgesJson
                ]);
                
                // 创建网络
                var container = document.getElementById('network');
                var data = {
                    nodes: nodes,
                    edges: edges
                };
                var options = {
                    nodes: {
                        shape: 'box',
                        font: {
                            size: 14
                        }
                    },
                    edges: {
                        font: {
                            size: 12,
                            align: 'middle'
                        },
                        smooth: {
                            type: 'continuous'
                        }
                    },
                    physics: {
                        stabilization: true,
                        barnesHut: {
                            gravitationalConstant: -2000,
                            centralGravity: 0.1,
                            springLength: 150,
                            springConstant: 0.05
                        }
                    },
                    groups: {
                        1: {color: {background: '#97C2FC', border: '#2B7CE9'}, borderWidth: 2}, // 孤立节点
                        2: {color: {background: '#FB7E81', border: '#FA0A10'}, borderWidth: 2}, // 主节点
                        3: {color: {background: '#7BE141', border: '#31B404'}, borderWidth: 2}, // 从节点
                        4: {color: {background: '#FFA807', border: '#FF8000'}, borderWidth: 2}, // 监督者
                        5: {color: {background: '#6E6EFD', border: '#0000FF'}, borderWidth: 2}  // 其他节点
                    },
                    interaction: {
                        hover: true,
                        navigationButtons: true,
                        keyboard: true
                    }
                };
                var network = new vis.Network(container, data, options);
                
                // 节点点击事件
                network.on("click", function(params) {
                    if (params.nodes.length > 0) {
                        var nodeId = params.nodes[0];
                        var node = nodes.get(nodeId);
                        var connectedNodes = network.getConnectedNodes(nodeId);
                        
                        var nodeInfo = document.getElementById('node-info');
                        nodeInfo.style.display = 'block';
                        nodeInfo.innerHTML = '<h3>Node: ' + node.label + '</h3>' +
                                            '<p>Connections: ' + node.connections + '</p>' +
                                            '<p>Connected to: ' + connectedNodes.join(', ') + '</p>';
                    }
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }
    
    /**
     * 保存网络可视化到文件
     *
     * @param topology 网络拓扑
     * @param outputPath 输出文件路径
     * @param format 输出格式，支持 "html" 和 "dot"
     * @param title 图表标题
     * @return 是否成功保存
     */
    fun saveVisualization(
        topology: NetworkTopology,
        outputPath: String,
        format: String = "html",
        title: String = "Agent Network"
    ): Boolean {
        try {
            val content = when (format.lowercase()) {
                "html" -> generateHtmlVisualization(topology, title)
                "dot" -> generateDotGraph(topology, title)
                else -> throw IllegalArgumentException("Unsupported format: $format")
            }
            
            File(outputPath).writeText(content)
            return true
        } catch (e: Exception) {
            println("Error saving visualization: ${e.message}")
            return false
        }
    }
    
    /**
     * 获取关系类型对应的颜色
     */
    private fun getRelationColor(relation: AgentRelation): String {
        return when (relation) {
            AgentRelation.PEER -> "blue"
            AgentRelation.MASTER -> "red"
            AgentRelation.SLAVE -> "green"
            AgentRelation.COLLABORATOR -> "purple"
            AgentRelation.SUPERVISOR -> "orange"
            AgentRelation.SUPERVISED -> "brown"
        }
    }
    
    /**
     * 获取关系类型对应的标签
     */
    private fun getRelationLabel(relation: AgentRelation): String {
        return when (relation) {
            AgentRelation.PEER -> "peer"
            AgentRelation.MASTER -> "master"
            AgentRelation.SLAVE -> "slave"
            AgentRelation.COLLABORATOR -> "collaborator"
            AgentRelation.SUPERVISOR -> "supervisor"
            AgentRelation.SUPERVISED -> "supervised"
        }
    }
}
