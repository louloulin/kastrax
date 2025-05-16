package ai.kastrax.code.ui.components

import ai.kastrax.code.model.Context
import ai.kastrax.code.model.ContextElement
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
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
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextPane
import javax.swing.ScrollPaneConstants
import javax.swing.text.html.HTMLEditorKit

/**
 * 上下文面板
 *
 * 显示当前上下文信息
 */
class ContextPanel(
    private val project: Project
) : JBPanel<JBPanel<*>>(BorderLayout()) {

    private val editorFactory = EditorFactory.getInstance()
    private val editors = mutableListOf<EditorEx>()
    private val elementsPanel = JBPanel<JBPanel<*>>()
    private val statusLabel = JBLabel("无上下文")

    init {
        border = JBUI.Borders.empty(8)

        // 设置元素面板
        elementsPanel.layout = BoxLayout(elementsPanel, BoxLayout.Y_AXIS)

        val scrollPane = JBScrollPane(elementsPanel)
        scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED

        // 创建状态面板
        val statusPanel = createStatusPanel()

        // 添加组件
        add(statusPanel, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
    }

    /**
     * 创建状态面板
     */
    private fun createStatusPanel(): JPanel {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
            JBUI.Borders.empty(4)
        )

        // 状态标签
        statusLabel.font = statusLabel.font.deriveFont(Font.BOLD)
        panel.add(statusLabel, BorderLayout.WEST)

        // 刷新按钮
        val refreshButton = JButton(AllIcons.Actions.Refresh)
        refreshButton.toolTipText = "刷新上下文"
        refreshButton.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
        refreshButton.addActionListener {
            // 刷新上下文逻辑
        }

        val actionsPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.RIGHT))
        actionsPanel.add(refreshButton)
        panel.add(actionsPanel, BorderLayout.EAST)

        return panel
    }

    /**
     * 设置上下文
     */
    fun setContext(context: Context) {
        // 清空现有元素
        elementsPanel.removeAll()

        // 释放编辑器
        disposeEditors()

        if (context.elements.isEmpty()) {
            statusLabel.text = "无上下文"
            elementsPanel.add(createEmptyMessage())
        } else {
            statusLabel.text = "上下文元素: ${context.elements.size}"

            // 添加上下文元素
            context.elements.forEach { element ->
                elementsPanel.add(createContextElementPanel(element))
                elementsPanel.add(Box.createVerticalStrut(8))
            }
        }

        elementsPanel.revalidate()
        elementsPanel.repaint()
    }

    /**
     * 创建空消息
     */
    private fun createEmptyMessage(): JComponent {
        val label = JBLabel("没有找到相关上下文")
        label.foreground = JBColor.gray
        label.horizontalAlignment = JBLabel.CENTER
        label.verticalAlignment = JBLabel.CENTER

        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.add(label, BorderLayout.CENTER)
        panel.preferredSize = java.awt.Dimension(0, 100)

        return panel
    }

    /**
     * 创建上下文元素面板
     */
    private fun createContextElementPanel(element: ContextElement): JComponent {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.border = BorderFactory.createLineBorder(JBColor.border())

        // 创建头部面板
        val headerPanel = JBPanel<JBPanel<*>>(BorderLayout())
        headerPanel.border = BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border())
        headerPanel.background = JBColor(Color(240, 240, 240), Color(60, 63, 65))

        // 元素类型和名称
        val titleLabel = JBLabel("${element.type}: ${element.name}")
        titleLabel.font = titleLabel.font.deriveFont(Font.BOLD)
        titleLabel.border = JBUI.Borders.empty(4)
        headerPanel.add(titleLabel, BorderLayout.WEST)

        // 位置信息
        val locationString = element.getLocationString()
        if (locationString.isNotEmpty()) {
            val locationLabel = JBLabel(locationString)
            locationLabel.font = locationLabel.font.deriveFont(Font.PLAIN, 10f)
            locationLabel.foreground = JBColor.gray
            locationLabel.border = JBUI.Borders.empty(4)
            headerPanel.add(locationLabel, BorderLayout.EAST)
        }

        panel.add(headerPanel, BorderLayout.NORTH)

        // 创建内容面板
        val contentPanel = createContentPanel(element)
        panel.add(contentPanel, BorderLayout.CENTER)

        return panel
    }

    /**
     * 创建内容面板
     */
    private fun createContentPanel(element: ContextElement): JComponent {
        // 检测内容类型
        return if (isCodeContent(element)) {
            createCodeEditor(element.content, getLanguageFromType(element.type))
        } else {
            createTextPane(element.content)
        }
    }

    /**
     * 判断是否为代码内容
     */
    private fun isCodeContent(element: ContextElement): Boolean {
        val codeTypes = setOf(
            "CLASS", "METHOD", "FUNCTION", "FIELD", "PROPERTY",
            "INTERFACE", "ENUM", "STRUCT", "CODE_SNIPPET"
        )
        return codeTypes.any { element.type.contains(it, ignoreCase = true) }
    }

    /**
     * 从类型获取语言
     */
    private fun getLanguageFromType(type: String): String {
        return when {
            type.contains("JAVA", ignoreCase = true) -> "java"
            type.contains("KOTLIN", ignoreCase = true) -> "kotlin"
            type.contains("PYTHON", ignoreCase = true) -> "python"
            type.contains("JAVASCRIPT", ignoreCase = true) -> "javascript"
            type.contains("TYPESCRIPT", ignoreCase = true) -> "typescript"
            type.contains("HTML", ignoreCase = true) -> "html"
            type.contains("CSS", ignoreCase = true) -> "css"
            type.contains("JSON", ignoreCase = true) -> "json"
            type.contains("XML", ignoreCase = true) -> "xml"
            else -> "text"
        }
    }

    /**
     * 创建文本显示区域
     */
    private fun createTextPane(text: String): JComponent {
        val textPane = JTextPane()

        // 支持HTML内容
        val htmlKit = HTMLEditorKit()
        textPane.editorKit = htmlKit

        textPane.contentType = "text/html"
        textPane.text = "<html><body style='font-family: Arial, sans-serif; font-size: 12pt;'>$text</body></html>"
        textPane.isEditable = false
        textPane.cursor = Cursor(Cursor.TEXT_CURSOR)

        return JBScrollPane(textPane).apply {
            border = BorderFactory.createEmptyBorder()
            preferredSize = java.awt.Dimension(0, minOf(text.lines().size * 20 + 30, 200))
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
        val height = minOf(lineCount * 20 + 30, 200) // 限制最大高度
        editor.component.preferredSize = java.awt.Dimension(0, height)

        // 添加到编辑器列表
        editors.add(editor)

        return editor.component
    }

    /**
     * 释放编辑器
     */
    private fun disposeEditors() {
        for (editor in editors) {
            editorFactory.releaseEditor(editor)
        }
        editors.clear()
    }

    /**
     * 释放资源
     */
    fun dispose() {
        disposeEditors()
    }
}
