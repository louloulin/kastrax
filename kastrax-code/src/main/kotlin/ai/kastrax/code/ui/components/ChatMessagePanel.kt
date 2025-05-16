package ai.kastrax.code.ui.components

import ai.kastrax.code.model.ChatMessage
import ai.kastrax.code.model.MessageRole
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.text.html.HTMLEditorKit

/**
 * 聊天消息面板
 * 
 * 显示单条聊天消息，包括用户消息和AI回复
 */
class ChatMessagePanel(private val message: ChatMessage) : JBPanel<ChatMessagePanel>(BorderLayout()) {
    
    init {
        border = JBUI.Borders.empty(8)
        
        // 创建消息头部（显示角色名称）
        val headerPanel = createHeaderPanel()
        add(headerPanel, BorderLayout.NORTH)
        
        // 创建消息内容
        val contentPanel = createContentPanel()
        add(contentPanel, BorderLayout.CENTER)
    }
    
    /**
     * 创建消息头部面板
     */
    private fun createHeaderPanel(): JPanel {
        val headerPanel = JBPanel<JBPanel<*>>(BorderLayout())
        headerPanel.border = JBUI.Borders.emptyBottom(4)
        
        val roleLabel = JBLabel(
            when (message.role) {
                MessageRole.USER -> "你"
                MessageRole.ASSISTANT -> "KastraX"
                MessageRole.SYSTEM -> "系统"
            }
        )
        
        roleLabel.font = roleLabel.font.deriveFont(Font.BOLD)
        headerPanel.add(roleLabel, BorderLayout.WEST)
        
        return headerPanel
    }
    
    /**
     * 创建消息内容面板
     */
    private fun createContentPanel(): JPanel {
        val contentPanel = JBPanel<JBPanel<*>>(BorderLayout())
        
        // 根据消息角色设置不同的背景色和边框
        val backgroundColor = when (message.role) {
            MessageRole.USER -> JBColor(Color(240, 240, 240), Color(60, 63, 65))
            else -> JBColor(Color(248, 250, 252), Color(49, 51, 53))
        }
        
        contentPanel.background = backgroundColor
        contentPanel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(JBColor(Color(220, 220, 220), Color(85, 85, 85)), 1),
            JBUI.Borders.empty(8)
        )
        
        // 创建文本区域
        val textPane = createTextPane(message.content)
        textPane.background = backgroundColor
        
        contentPanel.add(JBScrollPane(textPane), BorderLayout.CENTER)
        
        return contentPanel
    }
    
    /**
     * 创建文本显示区域
     */
    private fun createTextPane(text: String): JTextPane {
        val textPane = JTextPane()
        
        // 支持HTML内容
        val htmlKit = HTMLEditorKit()
        textPane.editorKit = htmlKit
        
        // 处理代码块
        val processedText = processCodeBlocks(text)
        
        textPane.contentType = "text/html"
        textPane.text = processedText
        textPane.isEditable = false
        textPane.cursor = Cursor(Cursor.TEXT_CURSOR)
        
        return textPane
    }
    
    /**
     * 处理文本中的代码块
     */
    private fun processCodeBlocks(text: String): String {
        // 简单的Markdown代码块处理
        val codeBlockPattern = "```(\\w*)\\s*\\n([\\s\\S]*?)```".toRegex()
        
        return text.replace(codeBlockPattern) { matchResult ->
            val language = matchResult.groupValues[1]
            val code = matchResult.groupValues[2]
            
            """
            <div style="background-color: #f5f5f5; border: 1px solid #ddd; border-radius: 3px; padding: 8px; margin: 8px 0; overflow-x: auto;">
                <pre style="margin: 0;"><code>${escapeHtml(code)}</code></pre>
            </div>
            """.trimIndent()
        }
    }
    
    /**
     * 转义HTML特殊字符
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
