package ai.kastrax.actor.monitoring

import ai.kastrax.actor.network.protocols.CollaborationResult
import ai.kastrax.actor.network.protocols.CollaborationStep
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 协作流程可视化工具，用于可视化协作协议中的消息流
 */
class CollaborationFlowVisualizer {
    
    /**
     * 生成协作流程的 Mermaid 图表
     *
     * @param result 协作结果
     * @param title 图表标题
     * @return Mermaid 格式的图表描述
     */
    fun generateMermaidSequenceDiagram(result: CollaborationResult, title: String = "Collaboration Flow"): String {
        val participants = result.participants
        val steps = result.steps
        
        val sb = StringBuilder()
        sb.appendLine("sequenceDiagram")
        sb.appendLine("  title: $title")
        
        // 添加参与者
        participants.forEach { participant ->
            sb.appendLine("  participant $participant")
        }
        
        // 添加消息流
        steps.forEachIndexed { index, step ->
            // 找出消息的发送者
            val sender = if (index == 0) {
                // 第一条消息的发送者是协作发起者
                participants.first()
            } else {
                // 其他消息的发送者是上一步的接收者
                steps[index - 1].agentId
            }
            
            // 消息接收者
            val receiver = step.agentId
            
            // 如果发送者和接收者相同，使用自循环
            if (sender == receiver) {
                sb.appendLine("  $sender->>$sender: Process task")
            } else {
                // 截断消息内容，避免图表过大
                val messageContent = truncateMessage(step.input)
                sb.appendLine("  $sender->>$receiver: $messageContent")
                
                // 添加响应
                val responseContent = truncateMessage(step.output)
                sb.appendLine("  $receiver-->>$sender: $responseContent")
            }
        }
        
        return sb.toString()
    }
    
    /**
     * 生成协作流程的 HTML 可视化
     *
     * @param result 协作结果
     * @param title 图表标题
     * @return HTML 格式的可视化
     */
    fun generateHtmlVisualization(result: CollaborationResult, title: String = "Collaboration Flow"): String {
        val mermaidDiagram = generateMermaidSequenceDiagram(result, title)
        
        // 准备步骤详情
        val stepsHtml = result.steps.mapIndexed { index, step ->
            """
            <div class="step-card">
                <h3>Step ${index + 1}: ${step.agentId}</h3>
                <div class="step-content">
                    <div class="step-input">
                        <h4>Input:</h4>
                        <pre>${escapeHtml(step.input)}</pre>
                    </div>
                    <div class="step-output">
                        <h4>Output:</h4>
                        <pre>${escapeHtml(step.output)}</pre>
                    </div>
                </div>
            </div>
            """.trimIndent()
        }.joinToString("\n")
        
        // 生成 HTML
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <title>$title</title>
            <script src="https://cdn.jsdelivr.net/npm/mermaid/dist/mermaid.min.js"></script>
            <style>
                body {
                    font-family: Arial, sans-serif;
                    margin: 0;
                    padding: 20px;
                    background-color: #f5f5f5;
                }
                .header {
                    background-color: #2c3e50;
                    color: white;
                    padding: 20px;
                    margin-bottom: 20px;
                    border-radius: 5px;
                }
                .header h1 {
                    margin: 0;
                }
                .header p {
                    margin: 5px 0 0 0;
                    opacity: 0.8;
                }
                .diagram-container {
                    background-color: white;
                    border-radius: 5px;
                    box-shadow: 0 2px 5px rgba(0,0,0,0.1);
                    padding: 20px;
                    margin-bottom: 20px;
                    overflow-x: auto;
                }
                .steps-container {
                    display: flex;
                    flex-direction: column;
                    gap: 20px;
                }
                .step-card {
                    background-color: white;
                    border-radius: 5px;
                    box-shadow: 0 2px 5px rgba(0,0,0,0.1);
                    padding: 15px;
                }
                .step-card h3 {
                    margin-top: 0;
                    color: #2c3e50;
                    border-bottom: 1px solid #eee;
                    padding-bottom: 10px;
                }
                .step-content {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 20px;
                }
                .step-input, .step-output {
                    flex: 1;
                    min-width: 300px;
                }
                .step-input h4, .step-output h4 {
                    color: #7f8c8d;
                    margin-bottom: 10px;
                }
                pre {
                    background-color: #f8f9fa;
                    border: 1px solid #eaecef;
                    border-radius: 3px;
                    padding: 10px;
                    overflow-x: auto;
                    white-space: pre-wrap;
                    word-wrap: break-word;
                }
                .summary {
                    background-color: white;
                    border-radius: 5px;
                    box-shadow: 0 2px 5px rgba(0,0,0,0.1);
                    padding: 15px;
                    margin-bottom: 20px;
                }
                .summary h2 {
                    margin-top: 0;
                    color: #2c3e50;
                    border-bottom: 1px solid #eee;
                    padding-bottom: 10px;
                }
                .summary-content {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 20px;
                }
                .summary-item {
                    flex: 1;
                    min-width: 200px;
                }
                .summary-item h3 {
                    color: #7f8c8d;
                    margin-bottom: 10px;
                }
                .footer {
                    text-align: center;
                    margin-top: 30px;
                    color: #7f8c8d;
                    font-size: 14px;
                }
            </style>
        </head>
        <body>
            <div class="header">
                <h1>$title</h1>
                <p>Generated at: ${DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.now())}</p>
            </div>
            
            <div class="summary">
                <h2>Collaboration Summary</h2>
                <div class="summary-content">
                    <div class="summary-item">
                        <h3>Status</h3>
                        <p>${if (result.success) "Success" else "Failed"}</p>
                    </div>
                    <div class="summary-item">
                        <h3>Participants</h3>
                        <p>${result.participants.joinToString(", ")}</p>
                    </div>
                    <div class="summary-item">
                        <h3>Steps</h3>
                        <p>${result.steps.size}</p>
                    </div>
                </div>
            </div>
            
            <div class="diagram-container">
                <div class="mermaid">
                $mermaidDiagram
                </div>
            </div>
            
            <h2>Detailed Steps</h2>
            <div class="steps-container">
                $stepsHtml
            </div>
            
            <div class="footer">
                Powered by Kastrax Actor Collaboration Flow Visualizer
            </div>
            
            <script>
                mermaid.initialize({
                    startOnLoad: true,
                    theme: 'default',
                    securityLevel: 'loose',
                    flowchart: {
                        useMaxWidth: true,
                        htmlLabels: true
                    },
                    sequence: {
                        diagramMarginX: 50,
                        diagramMarginY: 10,
                        boxTextMargin: 5,
                        noteMargin: 10,
                        messageMargin: 35,
                        mirrorActors: false
                    }
                });
            </script>
        </body>
        </html>
        """.trimIndent()
    }
    
    /**
     * 保存协作流程可视化到文件
     *
     * @param result 协作结果
     * @param outputPath 输出文件路径
     * @param format 输出格式，支持 "html" 和 "mermaid"
     * @param title 图表标题
     * @return 是否成功保存
     */
    fun saveVisualization(
        result: CollaborationResult,
        outputPath: String,
        format: String = "html",
        title: String = "Collaboration Flow"
    ): Boolean {
        try {
            val content = when (format.lowercase()) {
                "html" -> generateHtmlVisualization(result, title)
                "mermaid" -> generateMermaidSequenceDiagram(result, title)
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
     * 截断消息内容，避免图表过大
     *
     * @param message 原始消息
     * @param maxLength 最大长度
     * @return 截断后的消息
     */
    private fun truncateMessage(message: String, maxLength: Int = 30): String {
        val firstLine = message.lines().firstOrNull() ?: ""
        val truncated = if (firstLine.length > maxLength) {
            firstLine.substring(0, maxLength) + "..."
        } else {
            firstLine
        }
        
        // 转义特殊字符
        return truncated
            .replace(":", "&#58;")
            .replace("\"", "&quot;")
    }
    
    /**
     * 转义 HTML 特殊字符
     *
     * @param text 原始文本
     * @return 转义后的文本
     */
    private fun escapeHtml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
