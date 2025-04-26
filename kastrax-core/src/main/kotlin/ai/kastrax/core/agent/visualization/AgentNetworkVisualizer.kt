package ai.kastrax.core.agent.visualization

import ai.kastrax.core.agent.AgentInteraction
import ai.kastrax.core.common.KastraXBase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 代理网络可视化器，用于生成代理交互的可视化表示
 */
class AgentNetworkVisualizer : KastraXBase(component = "VISUALIZER", name = "agent-network") {
    private val interactions = mutableListOf<VisualizedInteraction>()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    /**
     * 记录代理开始执行
     */
    fun recordAgentStart(agentId: String, input: String) {
        interactions.add(
            VisualizedInteraction(
                type = InteractionType.START,
                agentId = agentId,
                timestamp = Instant.now(),
                content = input
            )
        )
        logger.debug("记录代理 '$agentId' 开始执行")
    }

    /**
     * 记录代理执行结束
     */
    fun recordAgentEnd(agentId: String, output: String) {
        interactions.add(
            VisualizedInteraction(
                type = InteractionType.END,
                agentId = agentId,
                timestamp = Instant.now(),
                content = output
            )
        )
        logger.debug("记录代理 '$agentId' 执行结束")
    }

    /**
     * 记录代理执行错误
     */
    fun recordAgentError(agentId: String, errorMessage: String) {
        interactions.add(
            VisualizedInteraction(
                type = InteractionType.ERROR,
                agentId = agentId,
                timestamp = Instant.now(),
                content = errorMessage
            )
        )
        logger.debug("记录代理 '$agentId' 执行错误")
    }

    /**
     * 生成可视化HTML
     */
    fun generateVisualization(agentHistory: Map<String, List<AgentInteraction>>): String {
        // 按时间戳排序交互
        val sortedInteractions = interactions.sortedBy { it.timestamp }
        
        // 获取所有代理ID
        val agentIds = agentHistory.keys.toList()
        
        // 生成HTML
        val html = buildString {
            append("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>代理网络可视化</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 20px; }
                        .timeline { position: relative; max-width: 1200px; margin: 0 auto; }
                        .timeline::after {
                            content: '';
                            position: absolute;
                            width: 6px;
                            background-color: #4CAF50;
                            top: 0;
                            bottom: 0;
                            left: 50%;
                            margin-left: -3px;
                        }
                        .container {
                            padding: 10px 40px;
                            position: relative;
                            background-color: inherit;
                            width: 50%;
                        }
                        .left { left: 0; }
                        .right { left: 50%; }
                        .container::after {
                            content: '';
                            position: absolute;
                            width: 25px;
                            height: 25px;
                            right: -17px;
                            background-color: white;
                            border: 4px solid #FF9F55;
                            top: 15px;
                            border-radius: 50%;
                            z-index: 1;
                        }
                        .left::after { right: -16px; }
                        .right::after { left: -16px; }
                        .content {
                            padding: 20px 30px;
                            background-color: white;
                            position: relative;
                            border-radius: 6px;
                            box-shadow: 0 2px 5px rgba(0,0,0,0.2);
                        }
                        .start { border-left: 5px solid #4CAF50; }
                        .end { border-left: 5px solid #2196F3; }
                        .error { border-left: 5px solid #F44336; }
                        .agent-list {
                            display: flex;
                            flex-wrap: wrap;
                            gap: 10px;
                            margin-bottom: 20px;
                        }
                        .agent-badge {
                            padding: 5px 10px;
                            background-color: #E0E0E0;
                            border-radius: 15px;
                            font-size: 14px;
                        }
                        pre {
                            background-color: #f5f5f5;
                            padding: 10px;
                            border-radius: 5px;
                            overflow-x: auto;
                            white-space: pre-wrap;
                        }
                        .timestamp {
                            color: #757575;
                            font-size: 12px;
                        }
                        h2 { color: #333; }
                        h3 { margin-top: 0; color: #555; }
                    </style>
                </head>
                <body>
                    <h1>代理网络交互可视化</h1>
                    
                    <h2>代理列表</h2>
                    <div class="agent-list">
            """.trimIndent())
            
            // 添加代理列表
            agentIds.forEach { agentId ->
                append("<div class=\"agent-badge\">$agentId</div>")
            }
            
            append("""
                    </div>
                    
                    <h2>交互时间线</h2>
                    <div class="timeline">
            """.trimIndent())
            
            // 添加交互时间线
            sortedInteractions.forEachIndexed { index, interaction ->
                val containerClass = if (index % 2 == 0) "container left" else "container right"
                val contentClass = when (interaction.type) {
                    InteractionType.START -> "content start"
                    InteractionType.END -> "content end"
                    InteractionType.ERROR -> "content error"
                }
                val typeLabel = when (interaction.type) {
                    InteractionType.START -> "开始"
                    InteractionType.END -> "结束"
                    InteractionType.ERROR -> "错误"
                }
                val formattedTime = dateFormatter.format(interaction.timestamp)
                
                append("""
                    <div class="$containerClass">
                        <div class="$contentClass">
                            <h3>代理: ${interaction.agentId} ($typeLabel)</h3>
                            <div class="timestamp">$formattedTime</div>
                            <pre>${interaction.content.escapeHtml()}</pre>
                        </div>
                    </div>
                """.trimIndent())
            }
            
            append("""
                    </div>
                    
                    <h2>代理历史记录</h2>
            """.trimIndent())
            
            // 添加代理历史记录
            agentIds.forEach { agentId ->
                val agentInteractions = agentHistory[agentId] ?: emptyList()
                if (agentInteractions.isNotEmpty()) {
                    append("""
                        <h3>代理: $agentId</h3>
                        <div style="margin-left: 20px;">
                    """.trimIndent())
                    
                    agentInteractions.forEachIndexed { index, interaction ->
                        val formattedTime = try {
                            val instant = Instant.parse(interaction.timestamp)
                            dateFormatter.format(instant)
                        } catch (e: Exception) {
                            interaction.timestamp
                        }
                        
                        append("""
                            <div style="margin-bottom: 20px; border-left: 3px solid #2196F3; padding-left: 10px;">
                                <div class="timestamp">$formattedTime</div>
                                <h4>输入:</h4>
                                <pre>${interaction.input.escapeHtml()}</pre>
                                <h4>输出:</h4>
                                <pre>${interaction.output.escapeHtml()}</pre>
                            </div>
                        """.trimIndent())
                    }
                    
                    append("</div>")
                }
            }
            
            append("""
                </body>
                </html>
            """.trimIndent())
        }
        
        return html
    }
    
    /**
     * 转义HTML特殊字符
     */
    private fun String.escapeHtml(): String {
        return this
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}

/**
 * 交互类型
 */
enum class InteractionType {
    START, END, ERROR
}

/**
 * 可视化交互
 */
data class VisualizedInteraction(
    val type: InteractionType,
    val agentId: String,
    val timestamp: Instant,
    val content: String
)
