package ai.kastrax.code.ui.components

import ai.kastrax.code.model.ChatMessage
import ai.kastrax.code.model.MessageRole
import ai.kastrax.code.mock.JBPanel
import ai.kastrax.code.mock.JBScrollPane
import ai.kastrax.code.mock.JBLabel
import ai.kastrax.code.mock.JBUI
import ai.kastrax.code.mock.JBColor
import ai.kastrax.code.mock.EditorFactory
import ai.kastrax.code.mock.EditorEx
import ai.kastrax.code.mock.FileTypeManager
import ai.kastrax.code.mock.Project
import ai.kastrax.code.mock.AllIcons
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.text.html.HTMLEditorKit

/**
 * 增强聊天消息面板
 *
 * 显示单条聊天消息，包括用户消息和AI回复，支持代码高亮和更多交互功能
 */
class EnhancedChatMessagePanel(
    private val message: ChatMessage,
    private val project: Project,
    private val onCodeSelected: (code: String, language: String) -> Unit
) : JBPanel<EnhancedChatMessagePanel>(BorderLayout()) {

    private val editorFactory = EditorFactory.getInstance()
    private val editors = mutableListOf<EditorEx>()

    init {
        border = JBUI.Borders.empty(8)

        // 创建消息头部（显示角色名称和操作按钮）
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

        // 角色标签
        val roleLabel = JBLabel(
            when (message.role) {
                MessageRole.USER -> "你"
                MessageRole.ASSISTANT -> "KastraX"
                MessageRole.SYSTEM -> "系统"
            }
        )

        roleLabel.font = roleLabel.font.deriveFont(Font.BOLD)
        headerPanel.add(roleLabel, BorderLayout.WEST)

        // 操作按钮面板
        val actionsPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.RIGHT, 4, 0))

        // 复制按钮
        val copyButton = JButton(AllIcons.Actions.Copy)
        copyButton.toolTipText = "复制消息内容"
        copyButton.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        copyButton.addActionListener {
            val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
            val selection = java.awt.datatransfer.StringSelection(message.content)
            clipboard.setContents(selection, null)
        }
        actionsPanel.add(copyButton)

        // 如果是助手消息，添加更多操作按钮
        if (message.role == MessageRole.ASSISTANT) {
            // 重新生成按钮
            val regenerateButton = JButton(AllIcons.Actions.Refresh)
            regenerateButton.toolTipText = "重新生成回复"
            regenerateButton.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
            regenerateButton.addActionListener {
                // 重新生成逻辑
            }
            actionsPanel.add(regenerateButton)

            // 保存按钮
            val saveButton = JButton(AllIcons.Actions.MenuSaveall)
            saveButton.toolTipText = "保存回复"
            saveButton.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
            saveButton.addActionListener {
                // 保存逻辑
            }
            actionsPanel.add(saveButton)
        }

        headerPanel.add(actionsPanel, BorderLayout.EAST)

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

        // 处理消息内容
        val processedContent = processMessageContent(message.content)
        contentPanel.add(processedContent, BorderLayout.CENTER)

        return contentPanel
    }

    /**
     * 处理消息内容
     */
    private fun processMessageContent(content: String): JPanel {
        val panel = JBPanel<JBPanel<*>>()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        // 检查是否包含代码块
        val codeBlockPattern = "```(\\w*)\\s*\\n([\\s\\S]*?)```".toRegex()
        val matches = codeBlockPattern.findAll(content)

        if (matches.count() > 0) {
            // 包含代码块，分段处理
            var lastEnd = 0

            for (match in matches) {
                // 添加代码块前的文本
                val beforeText = content.substring(lastEnd, match.range.first)
                if (beforeText.isNotBlank()) {
                    panel.add(createTextPane(beforeText))
                    panel.add(Box.createVerticalStrut(8))
                }

                // 添加代码块
                val language = match.groupValues[1].ifBlank { "text" }
                val code = match.groupValues[2]
                panel.add(createCodeEditor(code, language))
                panel.add(Box.createVerticalStrut(8))

                lastEnd = match.range.last + 1
            }

            // 添加最后一个代码块后的文本
            val afterText = content.substring(lastEnd)
            if (afterText.isNotBlank()) {
                panel.add(createTextPane(afterText))
            }
        } else {
            // 不包含代码块，直接显示文本
            panel.add(createTextPane(content))
        }

        return panel
    }

    /**
     * 创建文本显示区域
     */
    private fun createTextPane(text: String): JComponent {
        val textPane = JTextPane()

        // 支持HTML内容
        val htmlKit = HTMLEditorKit()
        textPane.editorKit = htmlKit

        // 处理Markdown格式
        val processedText = processMarkdown(text)

        textPane.contentType = "text/html"
        textPane.text = processedText
        textPane.isEditable = false
        textPane.cursor = Cursor(Cursor.TEXT_CURSOR)

        // 设置背景色透明
        textPane.isOpaque = false

        // 自动调整大小
        textPane.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, true)

        return JBScrollPane(textPane).apply {
            border = BorderFactory.createEmptyBorder()
            isOpaque = false
            viewport.isOpaque = false
        }
    }

    /**
     * 创建代码编辑器
     */
    private fun createCodeEditor(code: String, language: String): JComponent {
        // 获取文件类型
        val fileType = when (language.lowercase()) {
            "kotlin" -> FileTypeManager.getInstance().findFileTypeByName("Kotlin")
            "java" -> FileTypeManager.getInstance().findFileTypeByName("JAVA")
            "python" -> FileTypeManager.getInstance().findFileTypeByName("Python")
            "javascript", "js" -> FileTypeManager.getInstance().findFileTypeByName("JavaScript")
            "typescript", "ts" -> FileTypeManager.getInstance().findFileTypeByName("TypeScript")
            "html" -> FileTypeManager.getInstance().findFileTypeByName("HTML")
            "css" -> FileTypeManager.getInstance().findFileTypeByName("CSS")
            "json" -> FileTypeManager.getInstance().findFileTypeByName("JSON")
            "xml" -> FileTypeManager.getInstance().findFileTypeByName("XML")
            else -> FileTypeManager.getInstance().findFileTypeByName("PLAIN_TEXT")
        } ?: FileTypeManager.getInstance().findFileTypeByName("PLAIN_TEXT")

        // 创建文档
        val document = editorFactory.createDocument(code)

        // 创建编辑器
        val editor = editorFactory.createEditor(document, project, fileType, true) as EditorEx

        // 设置编辑器选项
        editor.settings.apply {
            isLineNumbersShown = true
            isLineMarkerAreaShown = true
            isFoldingOutlineShown = true
            isRightMarginShown = false
            additionalLinesCount = 0
            additionalColumnsCount = 0
        }

        // 设置编辑器大小
        val lineCount = code.lines().size
        val height = minOf(lineCount * 20 + 30, 300) // 限制最大高度
        editor.component.preferredSize = java.awt.Dimension(0, height)

        // 添加到编辑器列表
        editors.add(editor)

        // 创建代码面板
        val codePanel = JBPanel<JBPanel<*>>(BorderLayout())

        // 创建代码头部面板
        val codeHeaderPanel = JBPanel<JBPanel<*>>(BorderLayout())
        codeHeaderPanel.border = BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border())

        // 语言标签
        val languageLabel = JBLabel(language.uppercase())
        languageLabel.font = languageLabel.font.deriveFont(Font.BOLD, 10f)
        languageLabel.foreground = JBColor(Color(100, 100, 100), Color(180, 180, 180))
        languageLabel.border = JBUI.Borders.empty(2, 4)
        codeHeaderPanel.add(languageLabel, BorderLayout.WEST)

        // 操作按钮面板
        val codeActionsPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.RIGHT, 4, 0))

        // 复制按钮
        val copyButton = JButton("复制")
        copyButton.addActionListener {
            val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
            val selection = java.awt.datatransfer.StringSelection(code)
            clipboard.setContents(selection, null)
        }
        codeActionsPanel.add(copyButton)

        // 插入按钮
        val insertButton = JButton("插入到编辑器")
        insertButton.addActionListener {
            // 插入到编辑器逻辑
        }
        codeActionsPanel.add(insertButton)

        // 在代码面板中显示按钮
        val showInPanelButton = JButton("在代码面板中显示")
        showInPanelButton.addActionListener {
            onCodeSelected(code, language)
        }
        codeActionsPanel.add(showInPanelButton)

        codeHeaderPanel.add(codeActionsPanel, BorderLayout.EAST)

        // 添加到代码面板
        codePanel.add(codeHeaderPanel, BorderLayout.NORTH)
        codePanel.add(editor.component, BorderLayout.CENTER)

        // 添加边框
        codePanel.border = BorderFactory.createLineBorder(JBColor.border())

        return codePanel
    }

    /**
     * 处理Markdown格式
     */
    private fun processMarkdown(text: String): String {
        var result = text

        // 处理粗体
        result = result.replace("\\*\\*(.*?)\\*\\*".toRegex(), "<b>$1</b>")

        // 处理斜体
        result = result.replace("\\*(.*?)\\*".toRegex(), "<i>$1</i>")

        // 处理链接
        result = result.replace("\\[(.*?)\\]\\((.*?)\\)".toRegex(), "<a href=\"$2\">$1</a>")

        // 处理标题
        result = result.replace("^# (.*?)$".toRegex(RegexOption.MULTILINE), "<h1>$1</h1>")
        result = result.replace("^## (.*?)$".toRegex(RegexOption.MULTILINE), "<h2>$1</h2>")
        result = result.replace("^### (.*?)$".toRegex(RegexOption.MULTILINE), "<h3>$1</h3>")

        // 处理列表
        result = result.replace("^- (.*?)$".toRegex(RegexOption.MULTILINE), "• $1<br>")

        // 处理段落
        result = result.replace("\n\n", "<br><br>")

        return "<html><body style='font-family: Arial, sans-serif; font-size: 12pt;'>$result</body></html>"
    }

    /**
     * 释放资源
     */
    fun dispose() {
        for (editor in editors) {
            editorFactory.releaseEditor(editor)
        }
        editors.clear()
    }
}
